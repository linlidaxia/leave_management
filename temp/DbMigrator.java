import java.sql.*;

public class DbMigrator {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:E:\\AIDev\\leave_management\\webapp\\data.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();

            // 1. 检查 employees 表是否已有 identity_id 列
            boolean hasIdentityId = false;
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(employees)");
            while (rs.next()) {
                if ("identity_id".equals(rs.getString("name"))) {
                    hasIdentityId = true;
                    break;
                }
            }

            if (!hasIdentityId) {
                System.out.println("Adding identity_id column to employees table...");
                stmt.execute("ALTER TABLE employees ADD COLUMN identity_id INTEGER REFERENCES employee_identities(id)");
                System.out.println("  -> Done.");
            } else {
                System.out.println("employees.identity_id already exists, skipping.");
            }

            // 2. 检查并创建索引
            boolean hasIndex = false;
            rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='index' AND name='idx_employees_identity'");
            if (rs.next()) {
                hasIndex = true;
            }
            if (!hasIndex) {
                System.out.println("Creating index idx_employees_identity...");
                stmt.execute("CREATE INDEX idx_employees_identity ON employees(identity_id)");
                System.out.println("  -> Done.");
            } else {
                System.out.println("Index idx_employees_identity already exists, skipping.");
            }

            // 3. 删除 schema.sql 末尾那个有问题的触发器 (RAISE(IGNORE) 会导致每次 INSERT 都忽略)
            rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='trigger' AND name='trg_add_identity_id'");
            if (rs.next()) {
                System.out.println("Dropping bad trigger trg_add_identity_id...");
                stmt.execute("DROP TRIGGER trg_add_identity_id");
                System.out.println("  -> Done.");
            }

            conn.commit();
            System.out.println("\nMigration completed successfully!");

            // 验证
            System.out.println("\n=== employees columns after migration ===");
            rs = stmt.executeQuery("PRAGMA table_info(employees)");
            while (rs.next()) {
                System.out.println("  " + rs.getString("name") + " | " + rs.getString("type"));
            }
        }
    }
}
