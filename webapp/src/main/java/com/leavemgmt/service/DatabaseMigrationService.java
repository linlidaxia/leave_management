package com.leavemgmt.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 数据库版本迁移服务
 * 自动检测数据库版本并执行对应的升级脚本
 */
@Service
public class DatabaseMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);
    private final JdbcTemplate jdbc;

    public DatabaseMigrationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 迁移脚本定义: version -> SQL
     * 新增迁移时, 在这里添加新的版本号和对应的 SQL
     */
    private static final List<Migration> MIGRATIONS = new ArrayList<>();

    static {
        // v1: 初始版本 (schema.sql 已包含的表)
        MIGRATIONS.add(new Migration(1, "创建 schema_version 表",
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                "  version INTEGER PRIMARY KEY," +
                "  description TEXT," +
                "  applied_at TEXT DEFAULT (datetime('now','localtime'))" +
                ")"));

        // v2: 添加 employee_identities 表和 employees.identity_id (兼容已有的数据库)
        MIGRATIONS.add(new Migration(2, "添加人员身份管理",
                "CREATE TABLE IF NOT EXISTS employee_identities (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  name TEXT NOT NULL," +
                "  code TEXT," +
                "  remark TEXT" +
                ")" +
                ";;" +
                "CREATE INDEX IF NOT EXISTS idx_employees_identity ON employees(identity_id)"));

        // v3: 确保 employees.identity_id 列存在 (SQLite 不支持 IF NOT EXISTS ADD COLUMN, 用 Java 处理)
        MIGRATIONS.add(new Migration(3, "确保 identity_id 列存在",
                "-- 跳过, 由 Java 代码检测处理"));

        // v4: 假别表增加"优先扣除公休"字段
        MIGRATIONS.add(new Migration(4, "假别增加优先扣除公休字段",
                "ALTER TABLE leave_types ADD COLUMN deduct_from_annual INTEGER DEFAULT 0" +
                ";;" +
                "UPDATE leave_types SET deduct_from_annual = 1 WHERE name LIKE '%事假%'"));
    }

    /**
     * 执行迁移: 检查当前版本, 按顺序执行未应用的迁移脚本
     */
    public void migrate() {
        ensureVersionTable();
        int currentVersion = getCurrentVersion();
        int targetVersion = getTargetVersion();

        if (currentVersion >= targetVersion) {
            log.info("数据库版本已是最新: v{}", currentVersion);
            return;
        }

        log.info("数据库需要升级: v{} -> v{}", currentVersion, targetVersion);

        List<Migration> pending = new ArrayList<>();
        for (Migration m : MIGRATIONS) {
            if (m.version > currentVersion) {
                pending.add(m);
            }
        }
        pending.sort(Comparator.comparingInt(m -> m.version));

        for (Migration m : pending) {
            log.info("执行迁移 v{}: {}", m.version, m.description);
            try {
                executeMigration(m);
                recordVersion(m.version, m.description);
                log.info("迁移 v{} 完成", m.version);
            } catch (Exception e) {
                log.error("迁移 v{} 失败: {}", m.version, m.description, e);
                throw new RuntimeException("数据库迁移失败: v" + m.version + " - " + m.description, e);
            }
        }

        log.info("数据库迁移完成, 当前版本: v{}", getTargetVersion());
    }

    /**
     * 获取当前数据库版本
     */
    public int getCurrentVersion() {
        try {
            Integer v = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(version), 0) FROM schema_version", Integer.class);
            return v != null ? v : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取目标版本 (迁移脚本中的最高版本号)
     */
    public int getTargetVersion() {
        return MIGRATIONS.stream().mapToInt(m -> m.version).max().orElse(0);
    }

    private void ensureVersionTable() {
        try {
            jdbc.execute(
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                "  version INTEGER PRIMARY KEY," +
                "  description TEXT," +
                "  applied_at TEXT DEFAULT (datetime('now','localtime'))" +
                ")");
        } catch (Exception e) {
            log.warn("创建 schema_version 表失败 (可能已存在): {}", e.getMessage());
        }
    }

    private void executeMigration(Migration m) {
        String[] statements = m.sql.split(";;");
        Connection conn = null;
        try {
            conn = jdbc.getDataSource().getConnection();
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(trimmed);
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                    if (msg.contains("duplicate column")) {
                        log.info("列已存在，跳过: {}", trimmed.substring(0, Math.min(60, trimmed.length())));
                        continue;
                    }
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void recordVersion(int version, String description) {
        jdbc.update("INSERT INTO schema_version (version, description) VALUES (?, ?)",
                version, description);
    }

    /**
     * 迁移脚本定义
     */
    static class Migration {
        final int version;
        final String description;
        final String sql;

        Migration(int version, String description, String sql) {
            this.version = version;
            this.description = description;
            this.sql = sql;
        }
    }
}
