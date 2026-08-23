package com.leavemgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 行政事业单位请销假管理系统 v2.0 (Web Edition)
 * Spring Boot + SQLite 单文件应用
 */
@SpringBootApplication
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
}
