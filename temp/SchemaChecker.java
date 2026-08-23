import java.sql.*;

public class SchemaChecker {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:E:\\AIDev\\leave_management\\webapp\\data.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            // Get all tables
            System.out.println("=== Tables ===");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }
            
            // Get schema for each table
            System.out.println("\n=== Table Schemas ===");
            rs = stmt.executeQuery("SELECT name, sql FROM sqlite_master WHERE type='table' ORDER BY name");
            while (rs.next()) {
                System.out.println("\n--- " + rs.getString("name") + " ---");
                System.out.println(rs.getString("sql"));
            }
            
            // Check employees table columns
            System.out.println("\n=== Employees Table Columns ===");
            rs = stmt.executeQuery("PRAGMA table_info(employees)");
            while (rs.next()) {
                System.out.println(rs.getString("name") + " | " + rs.getString("type") + " | nullable:" + (rs.getInt("notnull") == 0 ? "YES" : "NO"));
            }
        }
    }
}
