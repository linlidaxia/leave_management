package com.leavemgmt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据库备份与恢复服务
 *
 * <p>SQLite 数据库文件位于 jar 同目录下的 data.db
 * <ul>
 *   <li>备份: 使用 SQLite 的 "backup to" 命令实现热备份 (无需停服); 失败时回退到文件复制</li>
 *   <li>恢复: 上传文件覆盖 data.db</li>
 * </ul>
 */
@Service
public class BackupService {

    private final JdbcTemplate jdbc;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    public BackupService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Path resolveDbPath() {
        // jdbc:sqlite:data.db -> data.db (相对工作目录)
        String url = datasourceUrl;
        String prefix = "jdbc:sqlite:";
        if (url.startsWith(prefix)) {
            String path = url.substring(prefix.length());
            // 去掉 query 参数
            int q = path.indexOf('?');
            if (q >= 0) path = path.substring(0, q);
            return Paths.get(path).toAbsolutePath();
        }
        return Paths.get("data.db").toAbsolutePath();
    }

    /**
     * 备份数据库到指定路径
     */
    public Path backup(String targetPath) throws IOException {
        Path source = resolveDbPath();
        Path target = Paths.get(targetPath).toAbsolutePath();
        Files.createDirectories(target.getParent());

        // 优先使用 SQLite 在线备份 API (热备份, 数据一致)
        try {
            jdbc.execute("VACUUM INTO '" + target.toString().replace("'", "''") + "'");
            return target;
        } catch (Exception ex) {
            // 回退到文件复制 (可能在备份瞬间数据正在写入, 不保证一致性)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
    }

    /**
     * 生成带时间戳的默认备份文件名
     */
    public String defaultBackupFileName() {
        return "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db";
    }

    /**
     * 从上传的文件恢复数据库 (覆盖当前 data.db)
     */
    public void restore(MultipartFile uploaded) throws IOException {
        Path dbPath = resolveDbPath();
        Path tmp = dbPath.resolveSibling(dbPath.getFileName() + ".restore_tmp");
        // 先写入临时文件
        try (var in = uploaded.getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        // 关闭连接池才能替换文件 — 这里使用 SQLite "restore from" 反向恢复
        try {
            jdbc.execute("RESTORE FROM '" + tmp.toString().replace("'", "''") + "'");
        } catch (Exception ex) {
            // 不支持 RESTORE 的旧驱动版本: 关闭后直接文件替换
            Files.copy(tmp, dbPath, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(tmp);
    }

    public String getCurrentDbPath() {
        return resolveDbPath().toString();
    }

    public long getCurrentDbSize() throws IOException {
        Path p = resolveDbPath();
        return Files.exists(p) ? Files.size(p) : 0;
    }
}
