package com.leavemgmt.controller;

import com.leavemgmt.model.*;
import com.leavemgmt.repository.LeaveAttachmentRepository;
import com.leavemgmt.service.LeaveService;
import com.leavemgmt.service.ExcelExportService;
import com.leavemgmt.service.DataImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请销假业务 REST 控制器
 *
 * <p>对应原 Python main.py 中的 10 个功能模块
 */
@RestController
@RequestMapping("/api")
public class LeaveController {

    private final LeaveService service;
    private final ExcelExportService excel;
    private final DataImportService importService;
    private final LeaveAttachmentRepository attachRepo;

    public LeaveController(LeaveService service, ExcelExportService excel,
                           DataImportService importService, LeaveAttachmentRepository attachRepo) {
        this.service = service;
        this.excel = excel;
        this.importService = importService;
        this.attachRepo = attachRepo;
    }

    // ==================== 首页概览 ====================

    @GetMapping("/dashboard")
    public DashboardStats dashboard() {
        return service.getDashboardStats();
    }

    // ==================== 部门管理 ====================

    @GetMapping("/departments")
    public Object listDepartments(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        java.util.List<Department> all = service.listDepartments();
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> saveDepartment(@RequestBody Department d) {
        Long id = service.saveDepartment(d);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        m.put("id", id);
        return m;
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> deleteDepartment(@PathVariable Long id) {
        try {
            service.deleteDepartment(id);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    // ==================== 人员身份管理 ====================

    @GetMapping("/identities")
    public List<com.leavemgmt.model.EmployeeIdentity> listIdentities() {
        return service.listIdentities();
    }

    @PostMapping("/identities")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> saveIdentity(@RequestBody com.leavemgmt.model.EmployeeIdentity ei) {
        try {
            Long id = service.saveIdentity(ei);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("id", id);
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @DeleteMapping("/identities/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> deleteIdentity(@PathVariable Long id) {
        try {
            service.deleteIdentity(id);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    // ==================== 人员管理 ====================

    @GetMapping("/employees")
    public Object listEmployees(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long identityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        java.util.List<java.util.Map<String, Object>> all = service.listEmployeesWithLeave(deptId, keyword, identityId);
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> saveEmployee(@RequestBody Employee e) {
        Long id = service.saveEmployee(e);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        m.put("id", id);
        return m;
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> deleteEmployee(@PathVariable Long id) {
        try {
            service.deleteEmployee(id);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    // ==================== 假别管理 ====================

    @GetMapping("/leave-types")
    public List<LeaveType> listLeaveTypes() {
        return service.listLeaveTypes();
    }

    @PostMapping("/leave-types")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> saveLeaveType(@RequestBody LeaveType lt) {
        try {
            Long id = service.saveLeaveType(lt);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("id", id);
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @DeleteMapping("/leave-types/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> deleteLeaveType(@PathVariable Long id) {
        try {
            service.deleteLeaveType(id);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    // ==================== 请假佐证附件 ====================

    /** 列出某请假记录下的所有附件 */
    @GetMapping("/applications/{appId}/attachments")
    public List<LeaveAttachment> listAttachments(@PathVariable Long appId) {
        return attachRepo.findByApplicationId(appId);
    }

    /** 上传附件 (multipart, 一次一个; 可多次调用上传多个) */
    @PostMapping(value = "/applications/{appId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> uploadAttachment(@PathVariable Long appId,
                                                 @RequestParam("file") MultipartFile file) {
        Map<String, Object> m = new HashMap<>();
        if (file == null || file.isEmpty()) {
            m.put("success", false);
            m.put("message", "请选择文件");
            return m;
        }
        // 限制大小: 单文件 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            m.put("success", false);
            m.put("message", "文件超过 10MB 限制");
            return m;
        }
        // 校验类型: 仅允许 PDF 或图片
        String ct = file.getContentType();
        String name = file.getOriginalFilename();
        if (ct == null) ct = "";
        String lowerName = name == null ? "" : name.toLowerCase();
        boolean isPdf = ct.equals("application/pdf") || lowerName.endsWith(".pdf");
        boolean isImage = ct.startsWith("image/") ||
                lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
        if (!isPdf && !isImage) {
            m.put("success", false);
            m.put("message", "仅支持 PDF 或图片格式 (jpg/png/gif/bmp/webp)");
            return m;
        }
        try {
            Long id = attachRepo.insert(appId, name, file.getSize(), ct, file.getBytes());
            m.put("success", true);
            m.put("id", id);
            m.put("fileName", name);
            m.put("fileSize", file.getSize());
        } catch (Exception e) {
            m.put("success", false);
            m.put("message", "上传失败: " + e.getMessage());
        }
        return m;
    }

    /** 下载附件 */
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) {
        LeaveAttachment a = attachRepo.findById(id);
        if (a == null) {
            return ResponseEntity.notFound().build();
        }
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(a.getFileName() == null ? "attachment" : a.getFileName(),
                    java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) { encoded = "attachment"; }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                a.getContentType() == null ? "application/octet-stream" : a.getContentType()));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(a.getFileData() == null ? 0 : a.getFileData().length);
        return new ResponseEntity<>(a.getFileData(), headers, org.springframework.http.HttpStatus.OK);
    }

    /** 在线预览附件 (inline, 用于浏览器直接打开 PDF/图片) */
    @GetMapping("/attachments/{id}/preview")
    public ResponseEntity<byte[]> previewAttachment(@PathVariable Long id) {
        LeaveAttachment a = attachRepo.findById(id);
        if (a == null) {
            return ResponseEntity.notFound().build();
        }
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(a.getFileName() == null ? "attachment" : a.getFileName(),
                    java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) { encoded = "attachment"; }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                a.getContentType() == null ? "application/octet-stream" : a.getContentType()));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(a.getFileData() == null ? 0 : a.getFileData().length);
        // 允许内嵌显示 (PDF / 图片)
        headers.add("X-Frame-Options", "SAMEORIGIN");
        return new ResponseEntity<>(a.getFileData(), headers, org.springframework.http.HttpStatus.OK);
    }

    /** 删除附件 */
    @DeleteMapping("/attachments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> deleteAttachment(@PathVariable Long id) {
        attachRepo.delete(id);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        return m;
    }

    // ==================== 批量导入 ====================

    /** 下载部门导入 Excel 模板 */
    @GetMapping("/import/departments/template")
    public ResponseEntity<byte[]> downloadDeptTemplate() throws IOException {
        byte[] data = importService.downloadDepartmentTemplate();
        return fileResponse(data, "部门导入模板.xlsx");
    }

    /** 上传部门 Excel 批量导入 */
    @PostMapping(value = "/import/departments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> importDepartments(@RequestParam("file") MultipartFile file) {
        Map<String, Object> m = new HashMap<>();
        if (file == null || file.isEmpty()) {
            m.put("success", false);
            m.put("message", "请选择要导入的文件");
            return ResponseEntity.badRequest().body(m);
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
            m.put("success", false);
            m.put("message", "仅支持 .xlsx / .xls 格式");
            return ResponseEntity.badRequest().body(m);
        }
        try (java.io.InputStream in = file.getInputStream()) {
            Map<String, Object> result = importService.importDepartments(in);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            m.put("success", false);
            m.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.ok(m);
        }
    }

    /** 下载人员导入 Excel 模板 */
    @GetMapping("/import/employees/template")
    public ResponseEntity<byte[]> downloadEmpTemplate() throws IOException {
        byte[] data = importService.downloadEmployeeTemplate();
        return fileResponse(data, "人员导入模板.xlsx");
    }

    /** 上传人员 Excel 批量导入 */
    @PostMapping(value = "/import/employees", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> importEmployees(@RequestParam("file") MultipartFile file) {
        Map<String, Object> m = new HashMap<>();
        if (file == null || file.isEmpty()) {
            m.put("success", false);
            m.put("message", "请选择要导入的文件");
            return ResponseEntity.badRequest().body(m);
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
            m.put("success", false);
            m.put("message", "仅支持 .xlsx / .xls 格式");
            return ResponseEntity.badRequest().body(m);
        }
        try (java.io.InputStream in = file.getInputStream()) {
            Map<String, Object> result = importService.importEmployees(in);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            m.put("success", false);
            m.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.ok(m);
        }
    }

    // ==================== 请假申请 ====================

    @GetMapping("/applications")
    public Object listApplications(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long identityId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Boolean annualRelated,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<LeaveApplication> all = service.listApplications(year, deptId, employeeId, leaveTypeId, status, identityId, startDate, endDate, annualRelated);
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    /** 计算请假天数与事假抵扣预览 */
    @GetMapping("/applications/preview")
    public Map<String, Object> previewApplication(
            @RequestParam Long empId,
            @RequestParam Long leaveTypeId,
            @RequestParam String startDate,
            @RequestParam String startPeriod,
            @RequestParam String endDate,
            @RequestParam String endPeriod) {
        return service.previewApplication(empId, leaveTypeId,
                LocalDate.parse(startDate), startPeriod,
                LocalDate.parse(endDate), endPeriod);
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> submitApplication(@RequestBody ApplicationRequest req) {
        try {
            Long id = service.submitApplication(req.employeeId, req.leaveTypeId,
                    LocalDate.parse(req.startDate), req.startPeriod,
                    LocalDate.parse(req.endDate), req.endPeriod,
                    req.reason);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("id", id);
            m.put("message", "请假登记成功");
            return m;
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @PutMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> updateApplication(@PathVariable Long id, @RequestBody ApplicationRequest req) {
        try {
            Long newId = service.updateApplication(id, req.employeeId, req.leaveTypeId,
                    LocalDate.parse(req.startDate), req.startPeriod,
                    LocalDate.parse(req.endDate), req.endPeriod,
                    req.reason);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("id", newId);
            return m;
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @PostMapping("/applications/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal(expression = "username") String approver) {
        try {
            service.approveApplication(id, approver);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("message", "审批通过");
            return m;
        } catch (IllegalStateException e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @PostMapping("/applications/batch-approve")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> batchApprove(@RequestBody List<Long> ids,
                                             @AuthenticationPrincipal(expression = "username") String approver) {
        try {
            service.batchApproveApplications(ids, approver);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("message", "批量审批成功，共审批 " + ids.size() + " 条记录");
            m.put("count", ids.size());
            return m;
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @PostMapping("/applications/batch-delete")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        try {
            service.batchDeleteApplications(ids);
            Map<String, Object> m = new HashMap<>();
            m.put("success", true);
            m.put("message", "批量删除成功，共删除 " + ids.size() + " 条记录");
            m.put("count", ids.size());
            return m;
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> deleteApplication(@PathVariable Long id) {
        service.deleteApplication(id);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        return m;
    }

    // ==================== 销假管理 ====================

    @GetMapping("/cancellations/pending")
    public Object pendingCancellations(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long identityId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        java.util.List<LeaveApplication> all;
        if (deptId == null && employeeId == null && leaveTypeId == null
                && identityId == null && year == null
                && (startDate == null || startDate.isBlank())
                && (endDate == null || endDate.isBlank())) {
            all = service.listPendingCancellations();
        } else {
            all = service.listPendingCancellationsFiltered(deptId, employeeId, leaveTypeId, startDate, endDate, identityId, year);
        }
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    @GetMapping("/cancellations")
    public Object listCancellations(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        java.util.List<LeaveCancellation> all = service.listCancellations();
        if (year != null) {
            final int y = year;
            all = all.stream()
                    .filter(c -> c.getStartDate() != null && c.getStartDate().getYear() == y)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    @PostMapping("/cancellations")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> registerCancellation(@RequestBody CancellationRequest req) {
        Long id = service.registerCancellation(req.applicationId,
                LocalDate.parse(req.cancelDate), req.actualDays, req.remark);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        m.put("id", id);
        return m;
    }

    // ==================== 公休假额度 ====================

    @GetMapping("/annual-balances")
    public Object listBalances(
            @RequestParam int year,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long identityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        java.util.List<AnnualLeaveBalance> all = service.listAnnualBalances(year, deptId, employeeId, identityId);
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    @PostMapping("/annual-balances/init")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> initBalance(@RequestParam int year) {
        int count = service.initAnnualBalance(year);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        m.put("count", count);
        m.put("message", "已初始化 " + count + " 名员工的 " + year + " 年度公休假额度");
        return m;
    }

    @PutMapping("/annual-balances/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Map<String, Object> updateBalance(@PathVariable Long id, @RequestBody BalanceRequest req) {
        service.updateBalance(id, req.totalDays, req.usedDays);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        return m;
    }

    // ==================== 统计 / 月度 / 汇总 ====================

    @GetMapping("/stats/statistics")
    public List<Map<String, Object>> statistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long deptId) {
        return service.getStatistics(year, deptId);
    }

    @GetMapping("/stats/monthly")
    public List<LeaveApplication> monthly(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long deptId) {
        return service.getMonthlySignData(year, month, deptId);
    }

    @GetMapping("/stats/summary")
    public List<Map<String, Object>> summary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long deptId) {
        return service.getSummaryData(year, deptId);
    }

    // ==================== 历史请假记录导入 ====================

    @GetMapping("/import/applications/template")
    public ResponseEntity<byte[]> downloadLeaveTemplate() throws IOException {
        byte[] data = importService.downloadLeaveImportTemplate();
        return fileResponse(data, "请假记录导入模板.xlsx");
    }

    @PostMapping(value = "/import/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> importLeaveApplications(@RequestParam("file") MultipartFile file) {
        Map<String, Object> m = new HashMap<>();
        if (file == null || file.isEmpty()) {
            m.put("success", false);
            m.put("message", "请选择要导入的文件");
            return ResponseEntity.badRequest().body(m);
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.toLowerCase().endsWith(".xlsx") || name.toLowerCase().endsWith(".xls"))) {
            m.put("success", false);
            m.put("message", "仅支持 .xlsx / .xls 格式");
            return ResponseEntity.badRequest().body(m);
        }
        try (java.io.InputStream in = file.getInputStream()) {
            Map<String, Object> result = importService.importLeaveApplications(in);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            m.put("success", false);
            m.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.ok(m);
        }
    }

    // ==================== Excel 导出 ====================

    @GetMapping("/export/annual-balance")
    public ResponseEntity<byte[]> exportBalance(@RequestParam int year,
                                                 @RequestParam(required = false) Long deptId) throws IOException {
        List<AnnualLeaveBalance> rows = service.listAnnualBalances(year, deptId);
        byte[] data = excel.exportAnnualBalance(year, rows);
        return fileResponse(data, "公休假额度表_" + year + ".xlsx");
    }

    @GetMapping("/export/statistics")
    public ResponseEntity<byte[]> exportStatistics(@RequestParam int year,
                                                    @RequestParam(required = false) Long deptId) throws IOException {
        List<Map<String, Object>> rows = service.getStatistics(year, deptId);
        byte[] data = excel.exportStatistics(year, rows);
        return fileResponse(data, "请假统计表_" + year + ".xlsx");
    }

    @GetMapping(value = "/applications/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> exportApplicationsExcel(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long leaveTypeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) throws IOException {
        Integer exportYear = year;
        if (exportYear == null) exportYear = java.time.LocalDate.now().getYear();
        List<LeaveApplication> rows = service.listApplications(exportYear, deptId, status);
        if (rows.size() > 20000) {
            rows = rows.subList(0, 20000);
        }
        byte[] data = excel.exportLeaveApplications(exportYear, rows);
        return fileResponse(data, "请假记录明细表_" + exportYear + ".xlsx");
    }

    @GetMapping("/export/monthly")
    public ResponseEntity<byte[]> exportMonthly(@RequestParam int year,
                                                  @RequestParam int month,
                                                  @RequestParam(required = false) Long deptId) throws IOException {
        List<LeaveApplication> rows = service.getMonthlySignData(year, month, deptId);
        byte[] data = excel.exportMonthlySign(year, month, rows);
        return fileResponse(data, "月度签字表_" + year + "年" + month + "月.xlsx");
    }

    @GetMapping("/export/summary")
    public ResponseEntity<byte[]> exportSummary(@RequestParam int year,
                                                 @RequestParam(required = false) Long deptId) throws IOException {
        List<Map<String, Object>> rows = service.getSummaryData(year, deptId);
        byte[] data = excel.exportSummary(year, rows);
        return fileResponse(data, "请假汇总表_" + year + ".xlsx");
    }

    private ResponseEntity<byte[]> fileResponse(byte[] data, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
    }

    // ==================== DTO ====================

    public static class ApplicationRequest {
        public Long employeeId;
        public Long leaveTypeId;
        public String startDate;
        public String startPeriod;
        public String endDate;
        public String endPeriod;
        public String reason;
    }

    public static class CancellationRequest {
        public Long applicationId;
        public String cancelDate;
        public double actualDays;
        public String remark;
    }

    public static class BalanceRequest {
        public double totalDays;
        public double usedDays;
    }
}
