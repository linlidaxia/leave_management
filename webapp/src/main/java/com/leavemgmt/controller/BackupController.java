package com.leavemgmt.controller;

import com.leavemgmt.service.BackupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 备份与恢复 REST 控制器
 */
@RestController
@RequestMapping("/api/backup")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/info")
    public Map<String, Object> info() throws IOException {
        Map<String, Object> m = new HashMap<>();
        m.put("dbPath", backupService.getCurrentDbPath());
        m.put("dbSize", backupService.getCurrentDbSize());
        m.put("dbType", "SQLite");
        return m;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download() throws IOException {
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"), backupService.defaultBackupFileName());
        backupService.backup(tmp.toString());
        byte[] data = java.nio.file.Files.readAllBytes(tmp);
        java.nio.file.Files.deleteIfExists(tmp);
        String filename = backupService.defaultBackupFileName();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
    }

    @PostMapping("/restore")
    public Map<String, Object> restore(@RequestParam("file") MultipartFile file) {
        Map<String, Object> m = new HashMap<>();
        if (file == null || file.isEmpty()) {
            m.put("success", false);
            m.put("message", "请选择备份文件");
            return m;
        }
        try {
            backupService.restore(file);
            m.put("success", true);
            m.put("message", "数据已恢复, 请重新登录");
            return m;
        } catch (Exception e) {
            m.put("success", false);
            m.put("message", "恢复失败: " + e.getMessage());
            return m;
        }
    }
}
