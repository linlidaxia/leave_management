package com.leavemgmt.service;

import com.leavemgmt.model.Department;
import com.leavemgmt.model.Employee;
import com.leavemgmt.model.LeaveApplication;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门/人员 Excel 批量导入服务
 *
 * <p>支持两种操作:
 * <ul>
 *   <li>下载 Excel 模板 (含表头说明 + 示例行)</li>
 *   <li>上传填好的 Excel 文件, 解析后批量导入</li>
 * </ul>
 */
@Service
public class DataImportService {

    private static final String FONT_NAME = "Microsoft YaHei";

    private final LeaveService leaveService;
    private final ExcelExportService excelExportService; // 复用样式工具

    public DataImportService(LeaveService leaveService, ExcelExportService excelExportService) {
        this.leaveService = leaveService;
        this.excelExportService = excelExportService;
    }

    // ==================== 部门模板 ====================

    public byte[] downloadDepartmentTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet("部门导入模板");

            // 标题行
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue("部门批量导入模板");
            title.setCellStyle(titleStyle(wb));

            // 表头
            String[] headers = {"部门名称*", "部门编号", "排序", "备注"};
            Row hr = ws.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle(wb));
            }

            // 示例数据 (2 行)
            String[][] samples = {
                {"办公室", "BGS", "1", "综合管理部门"},
                {"人事科", "RSK", "2", "人事管理"}
            };
            for (int i = 0; i < samples.length; i++) {
                Row r = ws.createRow(2 + i);
                for (int j = 0; j < samples[i].length; j++) {
                    Cell c = r.createCell(j);
                    c.setCellValue(samples[i][j]);
                    c.setCellStyle(cellStyle(wb));
                }
            }

            // 说明行
            Row noteRow = ws.createRow(5);
            Cell note = noteRow.createCell(0);
            note.setCellValue("说明: 1)带*列为必填项 2)部门名称不能重复 3)排序为数字 4)首行表头不可删除 5)示例数据(2-3行)可删除");
            note.setCellStyle(noteStyle(wb));

            ws.setColumnWidth(0, 24 * 512);
            ws.setColumnWidth(1, 14 * 512);
            ws.setColumnWidth(2, 10 * 512);
            ws.setColumnWidth(3, 30 * 512);
            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 解析部门 Excel 并批量导入
     * @return 导入结果 {total, success, failed, errors}
     */
    public Map<String, Object> importDepartments(InputStream in) throws IOException {
        Map<String, Object> ctx = new HashMap<>();
        return importExcel(in, "部门", (row, rowNum, context) -> parseAndSaveDepartment(row, rowNum, context), ctx);
    }

    private ParseResult parseAndSaveDepartment(Row row, int rowNum, Map<String, Object> ctx) {
        ParseResult r = new ParseResult();
        String name = getCellString(row, 0).trim();
        if (name.isEmpty()) {
            r.fail("部门名称不能为空");
            return r;
        }
        String code = getCellString(row, 1).trim();
        int sort = getCellInt(row, 2, 0);
        String remark = getCellString(row, 3).trim();

        Department d = new Department();
        d.setName(name);
        d.setCode(code);
        d.setSortOrder(sort);
        d.setRemark(remark);
        try {
            Long id = leaveService.saveDepartment(d);
            r.ok("已添加部门「" + name + "」 ID=" + id);
        } catch (Exception e) {
            r.fail("保存失败: " + e.getMessage());
        }
        return r;
    }

    // ==================== 人员模板 ====================

    public byte[] downloadEmployeeTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet("人员导入模板");

            // 标题行
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue("人员批量导入模板");
            title.setCellStyle(titleStyle(wb));

            // 表头
            String[] headers = {"姓名*", "性别", "身份证号", "部门名称*", "人员身份", "职务", "参加工作时间*", "电话", "备注"};
            Row hr = ws.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle(wb));
            }

            // 示例数据
            String[][] samples = {
                {"张三", "男", "110101198001010001", "办公室", "干部", "科员", "2010-03-15", "13800000001", "工龄15年"},
                {"李四", "女", "110101199501010002", "人事科", "职工", "科员", "2020-07-01", "13800000002", ""}
            };
            for (int i = 0; i < samples.length; i++) {
                Row r = ws.createRow(2 + i);
                for (int j = 0; j < samples[i].length; j++) {
                    Cell c = r.createCell(j);
                    c.setCellValue(samples[i][j]);
                    c.setCellStyle(cellStyle(wb));
                }
            }

            // 说明行
            Row noteRow = ws.createRow(5);
            Cell note = noteRow.createCell(0);
            note.setCellValue("说明: 1)带*列为必填 2)性别:男/女 3)部门名称需与系统中已有部门名称一致 4)人员身份需与系统中已有身份一致(可为空) 5)参加工作时间格式: YYYY-MM-DD 6)首行表头不可删除");
            note.setCellStyle(noteStyle(wb));

            int[] widths = {12, 8, 22, 16, 14, 12, 16, 14, 24};
            for (int i = 0; i < widths.length; i++) ws.setColumnWidth(i, widths[i] * 512);
            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 解析人员 Excel 并批量导入
     * @return 导入结果
     */
    public Map<String, Object> importEmployees(InputStream in) throws IOException {
        // 先把现有部门名称 -> ID 映射加载到上下文
        Map<String, Object> ctx = new HashMap<>();
        Map<String, Long> deptName2Id = new HashMap<>();
        for (Department d : leaveService.listDepartments()) {
            deptName2Id.put(d.getName(), d.getId());
        }
        ctx.put("deptName2Id", deptName2Id);
        // 人员身份名称 -> ID 映射
        Map<String, Long> identityName2Id = new HashMap<>();
        for (com.leavemgmt.model.EmployeeIdentity ei : leaveService.listIdentities()) {
            identityName2Id.put(ei.getName(), ei.getId());
        }
        ctx.put("identityName2Id", identityName2Id);

        return importExcel(in, "人员", (row, rowNum, context) -> parseAndSaveEmployee(row, rowNum, context), ctx);
    }

    @SuppressWarnings("unchecked")
    private ParseResult parseAndSaveEmployee(Row row, int rowNum, Map<String, Object> ctx) {
        ParseResult r = new ParseResult();
        String name = getCellString(row, 0).trim();
        if (name.isEmpty()) {
            r.fail("姓名不能为空");
            return r;
        }
        String gender = getCellString(row, 1).trim();
        if (gender.isEmpty()) gender = "男";
        String idCard = getCellString(row, 2).trim();
        String deptName = getCellString(row, 3).trim();
        if (deptName.isEmpty()) {
            r.fail("部门名称不能为空");
            return r;
        }
        Map<String, Long> deptName2Id = (Map<String, Long>) ctx.get("deptName2Id");
        Long deptId = deptName2Id.get(deptName);
        if (deptId == null) {
            r.fail("部门「" + deptName + "」不存在, 请先在部门管理中添加");
            return r;
        }
        String identityName = getCellString(row, 4).trim();
        Long identityId = null;
        if (!identityName.isEmpty()) {
            Map<String, Long> identityName2Id = (Map<String, Long>) ctx.get("identityName2Id");
            identityId = identityName2Id.get(identityName);
            if (identityId == null) {
                r.fail("人员身份「" + identityName + "」不存在, 请先在人员身份管理中添加");
                return r;
            }
        }
        String position = getCellString(row, 5).trim();
        String workStartStr = getCellString(row, 6).trim();
        if (workStartStr.isEmpty()) {
            r.fail("参加工作时间不能为空");
            return r;
        }
        LocalDate workStart;
        try {
            workStart = LocalDate.parse(workStartStr.length() >= 10 ? workStartStr.substring(0, 10) : workStartStr);
        } catch (Exception e) {
            r.fail("参加工作时间格式错误: " + workStartStr + " (应为 YYYY-MM-DD)");
            return r;
        }
        String phone = getCellString(row, 7).trim();
        String remark = getCellString(row, 8).trim();

        Employee e = new Employee();
        e.setName(name);
        e.setGender(gender);
        e.setIdCard(idCard);
        e.setDepartmentId(deptId);
        e.setIdentityId(identityId);
        e.setPosition(position);
        e.setWorkStartDate(workStart);
        e.setPhone(phone);
        e.setRemark(remark);
        try {
            Long id = leaveService.saveEmployee(e);
            r.ok("已添加人员「" + name + "」 ID=" + id);
        } catch (Exception ex) {
            r.fail("保存失败: " + ex.getMessage());
        }
        return r;
    }

    // ==================== 历史请假记录模板 ====================

    public byte[] downloadLeaveImportTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet("请假记录导入模板");

            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue("历史请假记录批量导入模板");
            title.setCellStyle(titleStyle(wb));

            String[] headers = {"员工姓名*", "假别名称*", "开始日期*", "开始时段*", "结束日期*", "结束时段*", "事由", "状态", "登记日期", "审批日期"};
            Row hr = ws.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle(wb));
            }

            String[][] samples = {
                {"张三", "病假", "2026-01-05", "上午", "2026-01-07", "下午", "感冒发烧", "已审批", "2026-01-05", "2026-01-05"},
                {"李四", "事假", "2026-03-10", "全天", "2026-03-10", "全天", "家中有事", "已销假", "2026-03-09", "2026-03-09"}
            };
            for (int i = 0; i < samples.length; i++) {
                Row r = ws.createRow(2 + i);
                for (int j = 0; j < samples[i].length; j++) {
                    Cell c = r.createCell(j);
                    c.setCellValue(samples[i][j]);
                    c.setCellStyle(cellStyle(wb));
                }
            }

            Row noteRow = ws.createRow(5);
            Cell note = noteRow.createCell(0);
            note.setCellValue("说明: 1)带*列为必填 2)员工姓名和假别名称需与系统中已有数据一致 3)日期格式:YYYY-MM-DD 4)时段:上午/下午/全天 5)状态:已审批/已销假(默认已审批) 6)导入的历史记录不扣减年假额度");
            note.setCellStyle(noteStyle(wb));

            int[] widths = {14, 12, 14, 10, 14, 10, 24, 10, 14, 14};
            for (int i = 0; i < widths.length; i++) ws.setColumnWidth(i, widths[i] * 512);
            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 导入历史请假记录
     * 不扣减年假额度 (历史数据), 直接插入记录
     */
    public Map<String, Object> importLeaveApplications(InputStream in) throws IOException {
        Map<String, Object> ctx = new HashMap<>();
        Map<String, Long> empName2Id = new HashMap<>();
        for (Employee e : leaveService.listEmployees(null, null)) {
            empName2Id.put(e.getName(), e.getId());
        }
        Map<String, Long> ltName2Id = new HashMap<>();
        for (com.leavemgmt.model.LeaveType lt : leaveService.listLeaveTypes()) {
            ltName2Id.put(lt.getName(), lt.getId());
        }
        ctx.put("empName2Id", empName2Id);
        ctx.put("ltName2Id", ltName2Id);

        return importExcel(in, "请假记录", (row, rowNum, context) -> parseAndSaveLeaveApplication(row, rowNum, context), ctx);
    }

    @SuppressWarnings("unchecked")
    private ParseResult parseAndSaveLeaveApplication(Row row, int rowNum, Map<String, Object> ctx) {
        ParseResult r = new ParseResult();
        String empName = getCellString(row, 0).trim();
        if (empName.isEmpty()) { r.fail("员工姓名不能为空"); return r; }
        String ltName = getCellString(row, 1).trim();
        if (ltName.isEmpty()) { r.fail("假别名称不能为空"); return r; }
        String startDateStr = getCellString(row, 2).trim();
        if (startDateStr.isEmpty()) { r.fail("开始日期不能为空"); return r; }
        String startPeriod = getCellString(row, 3).trim();
        if (startPeriod.isEmpty()) startPeriod = "全天";
        String endDateStr = getCellString(row, 4).trim();
        if (endDateStr.isEmpty()) { r.fail("结束日期不能为空"); return r; }
        String endPeriod = getCellString(row, 5).trim();
        if (endPeriod.isEmpty()) endPeriod = "全天";
        String reason = getCellString(row, 6).trim();
        String status = getCellString(row, 7).trim();
        if (status.isEmpty()) status = "已审批";
        String applyDateStr = getCellString(row, 8).trim();
        String approveDateStr = getCellString(row, 9).trim();

        Map<String, Long> empName2Id = (Map<String, Long>) ctx.get("empName2Id");
        Map<String, Long> ltName2Id = (Map<String, Long>) ctx.get("ltName2Id");
        Long empId = empName2Id.get(empName);
        if (empId == null) { r.fail("员工「" + empName + "」不存在"); return r; }
        Long ltId = ltName2Id.get(ltName);
        if (ltId == null) { r.fail("假别「" + ltName + "」不存在"); return r; }

        LocalDate startDate, endDate;
        try { startDate = LocalDate.parse(startDateStr.length() >= 10 ? startDateStr.substring(0, 10) : startDateStr); }
        catch (Exception e) { r.fail("开始日期格式错误: " + startDateStr); return r; }
        try { endDate = LocalDate.parse(endDateStr.length() >= 10 ? endDateStr.substring(0, 10) : endDateStr); }
        catch (Exception e) { r.fail("结束日期格式错误: " + endDateStr); return r; }

        double days = com.leavemgmt.util.LeaveCalculator.calculateLeaveDays(startDate, startPeriod, endDate, endPeriod);
        if (days <= 0) { r.fail("请假天数计算为0, 请检查日期和时段"); return r; }

        LocalDate applyDate = LocalDate.now();
        if (!applyDateStr.isEmpty()) {
            try { applyDate = LocalDate.parse(applyDateStr.length() >= 10 ? applyDateStr.substring(0, 10) : applyDateStr); }
            catch (Exception e) { /* ignore, use today */ }
        }
        LocalDate approveDate = applyDate;
        if (!approveDateStr.isEmpty()) {
            try { approveDate = LocalDate.parse(approveDateStr.length() >= 10 ? approveDateStr.substring(0, 10) : approveDateStr); }
            catch (Exception e) { /* ignore, use applyDate */ }
        }

        LeaveApplication a = new LeaveApplication();
        a.setEmployeeId(empId);
        a.setLeaveTypeId(ltId);
        a.setStartDate(startDate);
        a.setStartPeriod(startPeriod);
        a.setEndDate(endDate);
        a.setEndPeriod(endPeriod);
        a.setDays(days);
        a.setReason(reason);
        a.setStatus(status);
        a.setApplyDate(applyDate);
        a.setOffsetAnnual(0.0);

        try {
            Long id = leaveService.insertHistoricalApplication(a, approveDate);
            r.ok("已导入「" + empName + "」" + ltName + " " + days + "天 ID=" + id);
        } catch (Exception e) {
            r.fail("保存失败: " + e.getMessage());
        }
        return r;
    }

    // ==================== 通用 Excel 解析 ====================

    @FunctionalInterface
    private interface RowParser {
        ParseResult parse(Row row, int rowNum, Map<String, Object> ctx) throws Exception;
    }

    private Map<String, Object> importExcel(InputStream in, String type, RowParser parser, Map<String, Object> ctx) throws IOException {
        Workbook wb = WorkbookFactory.create(in);
        Sheet sheet = wb.getSheetAt(0);
        Map<String, Object> result = new HashMap<>();
        int total = 0, success = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        List<String> details = new ArrayList<>();

        // 数据从第 2 行开始 (第 0 行标题, 第 1 行表头)
        int firstDataRow = 2;
        int lastRow = sheet.getLastRowNum();
        for (int i = firstDataRow; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            // 跳过完全空行
            if (isRowEmpty(row)) continue;
            // 跳过说明行 (首列以"说明"开头)
            String firstCell = getCellString(row, 0).trim();
            if (firstCell.startsWith("说明")) continue;

            total++;
            try {
                ParseResult r = parser.parse(row, i, ctx);
                if (r.success) {
                    success++;
                    details.add("第" + (i + 1) + "行: " + r.message);
                } else {
                    failed++;
                    String msg = "第" + (i + 1) + "行: " + r.message;
                    errors.add(msg);
                    details.add("【失败】" + msg);
                }
            } catch (Exception e) {
                failed++;
                String msg = "第" + (i + 1) + "行: 解析异常 - " + e.getMessage();
                errors.add(msg);
                details.add("【异常】" + msg);
            }
        }
        wb.close();

        result.put("type", type);
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        result.put("details", details);
        return result;
    }

    private static boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null) {
                String v = getCellString(c).trim();
                if (!v.isEmpty()) return false;
            }
        }
        return true;
    }

    private static String getCellString(Row row, int col) {
        if (row == null) return "";
        Cell c = row.getCell(col);
        return c == null ? "" : getCellString(c);
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v)) return String.valueOf((long) v);
                return String.valueOf(v);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); }
                catch (Exception e) {
                    try { return String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e2) { return ""; }
                }
            default: return "";
        }
    }

    private static int getCellInt(Row row, int col, int def) {
        String s = getCellString(row, col).trim();
        if (s.isEmpty()) return def;
        try { return (int) Double.parseDouble(s); }
        catch (Exception e) { return def; }
    }

    // ==================== 样式 ====================

    private CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME); f.setFontHeightInPoints((short) 14); f.setBold(true);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }
    private CellStyle headStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME); f.setFontHeightInPoints((short) 10); f.setBold(true);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        thinBorder(s); return s;
    }
    private CellStyle cellStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME); f.setFontHeightInPoints((short) 10);
        s.setFont(f); s.setAlignment(HorizontalAlignment.CENTER);
        thinBorder(s); return s;
    }
    private CellStyle noteStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME); f.setFontHeightInPoints((short) 9); f.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFont(f); s.setAlignment(HorizontalAlignment.LEFT);
        return s;
    }
    private void thinBorder(CellStyle s) {
        BorderStyle b = BorderStyle.THIN;
        s.setBorderLeft(b); s.setBorderRight(b);
        s.setBorderTop(b); s.setBorderBottom(b);
    }

    // ==================== 内部辅助类 ====================

    private static class ParseResult {
        boolean success;
        String message;
        void ok(String m)  { this.success = true;  this.message = m; }
        void fail(String m) { this.success = false; this.message = m; }
    }
}
