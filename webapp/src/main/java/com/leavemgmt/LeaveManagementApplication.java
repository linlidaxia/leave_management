package com.leavemgmt;

import com.leavemgmt.service.DatabaseMigrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 行政事业单位请销假管理系统 v2.0 (Web Edition)
 * Spring Boot + SQLite 单文件应用
 */
@SpringBootApplication
@EnableScheduling
public class LeaveManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveManagementApplication.class, args);
        System.out.println();
        System.out.println("==================================================");
        System.out.println("  Leave Management System v2.0 started");
        System.out.println("  URL:   http://localhost:9000");
        System.out.println("  Login: admin / admin123");
        System.out.println("==================================================");
        System.out.println();
    }

    @Bean
    public CommandLineRunner runMigration(DatabaseMigrationService migrationService) {
        return args -> {
            System.out.println(">>> 检查数据库版本...");
            migrationService.migrate();
            System.out.println(">>> 数据库版本: v" + migrationService.getCurrentVersion());
        };
    }
}
