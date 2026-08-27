package com.leavemgmt.service;

import com.leavemgmt.model.AnnualLeaveBalance;
import com.leavemgmt.model.LeaveApplication;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出服务 (Apache POI)
 *
 * <p>对应原 Python 项目 main.py 中的 4 个导出功能:
 * <ul>
 *   <li>公休假额度表 (_balance_export)</li>
 *   <li>统计表 (_stats_export)</li>
 *   <li>月度签字表 (_monthly_export)</li>
 *   <li>汇总表 (_summary_export)</li>
 * </ul>
 */
@Service
public class ExcelExportService {

    private static final String FONT_NAME = "SimSun";

    public byte[] exportAnnualBalance(int year, List<AnnualLeaveBalance> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet(year + "年公休假额度");
            // 标题
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue(year + "年度公休假额度表");
            title.setCellStyle(titleStyle(wb));

            // 表头
            String[] headers = {"ID", "姓名", "部门", "工龄(年)", "总额度(天)", "已用(天)", "剩余(天)"};
            Row hr = ws.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle(wb));
            }
            // 数据
            int r = 2;
            for (AnnualLeaveBalance b : rows) {
                Row row = ws.createRow(r++);
                setCell(row, 0, b.getId(), wb);
                setCell(row, 1, b.getEmployeeName(), wb);
                setCell(row, 2, b.getDepartmentName() == null ? "" : b.getDepartmentName(), wb);
                setCell(row, 3, b.getWorkYears(), wb);
                setCell(row, 4, b.getTotalDays(), wb);
                setCell(row, 5, b.getUsedDays(), wb);
                setCell(row, 6, b.getRemainingDays(), wb);
            }
            for (int i = 0; i < 7; i++) ws.setColumnWidth(i, 12 * 512);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportStatistics(int year, List<Map<String, Object>> rows) throws IOException {
        // Collect leave types in order
        java.util.LinkedHashSet<String> ltSet = new java.util.LinkedHashSet<>();
        for (Map<String, Object> r : rows) ltSet.add(asStr(r.get("leave_type")));
        java.util.List<String> ltList = new java.util.ArrayList<>(ltSet);

        // Pivot: key = empId_empName_deptName
        java.util.LinkedHashMap<String, Object[]> empMap = new java.util.LinkedHashMap<>();
        java.util.List<String> empOrder = new java.util.ArrayList<>();
        for (Map<String, Object> r : rows) {
            String key = r.get("emp_id") + "_" + asStr(r.get("emp_name")) + "_" + asStr(r.get("dept_name"));
            if (!empMap.containsKey(key)) {
                empMap.put(key, new Object[]{asStr(r.get("emp_name")), asStr(r.get("dept_name")), new java.util.HashMap<String, double[]>(){{
                    for (String lt : ltList) put(lt, new double[]{0, 0});
                }}});
                empOrder.add(key);
            }
            String lt = asStr(r.get("leave_type"));
            double[] vals = ((java.util.Map<String, double[]>) empMap.get(key)[2]).get(lt);
            vals[0] += asDouble(r.get("total_days"));
            vals[1] += asLong(r.get("cnt"));
        }

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet(year + "年统计");

            // Title row
            int totalCols = 2 + ltList.size() * 2 + 1;
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue(year + "年度请假统计表");
            title.setCellStyle(titleStyle(wb));

            // Row 1: header row 1
            Row hr1 = ws.createRow(1);
            Cell c0 = hr1.createCell(0);
            c0.setCellValue("姓名");
            c0.setCellStyle(headStyle(wb));
            ws.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));

            Cell c1 = hr1.createCell(1);
            c1.setCellValue("部门");
            c1.setCellStyle(headStyle(wb));
            ws.addMergedRegion(new CellRangeAddress(1, 2, 1, 1));

            int col = 2;
            for (String lt : ltList) {
                Cell clt = hr1.createCell(col);
                clt.setCellValue(lt);
                clt.setCellStyle(headStyle(wb));
                ws.addMergedRegion(new CellRangeAddress(1, 1, col, col + 1));
                col += 2;
            }

            Cell cTotal = hr1.createCell(col);
            cTotal.setCellValue("合计天数");
            cTotal.setCellStyle(headStyle(wb));
            ws.addMergedRegion(new CellRangeAddress(1, 2, col, col));

            // Row 2: sub header
            Row hr2 = ws.createRow(2);
            // name/dept cells are merged from row1, leave empty
            col = 2;
            for (int i = 0; i < ltList.size(); i++) {
                Cell cCnt = hr2.createCell(col);
                cCnt.setCellValue("次数");
                cCnt.setCellStyle(headStyle(wb));
                Cell cDays = hr2.createCell(col + 1);
                cDays.setCellValue("天数");
                cDays.setCellStyle(headStyle(wb));
                col += 2;
            }

            // Data rows
            int r = 3;
            for (String empKey : empOrder) {
                Object[] empData = empMap.get(empKey);
                @SuppressWarnings("unchecked")
                java.util.Map<String, double[]> ltData = (java.util.Map<String, double[]>) empData[2];
                Row x = ws.createRow(r++);
                setCell(x, 0, empData[0], wb); // name
                setCell(x, 1, empData[1], wb); // dept
                int c = 2;
                double totalDays = 0;
                for (String lt : ltList) {
                    double[] vals = ltData.get(lt);
                    setCell(x, c, vals[1] > 0 ? vals[1] : "", wb);
                    setCell(x, c + 1, vals[0] > 0 ? vals[0] : "", wb);
                    totalDays += vals[0];
                    c += 2;
                }
                setCell(x, c, totalDays, wb);
            }

            // Column widths
            ws.setColumnWidth(0, 12 * 512);
            ws.setColumnWidth(1, 16 * 512);
            for (int i = 0; i < ltList.size(); i++) {
                ws.setColumnWidth(2 + i * 2, 8 * 512);
                ws.setColumnWidth(3 + i * 2, 10 * 512);
            }
            ws.setColumnWidth(totalCols - 1, 12 * 512);

            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportMonthlySign(int year, int month, List<LeaveApplication> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet(year + "年" + month + "月签字表");
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue(year + "年" + month + "月请假签字确认表");
            title.setCellStyle(titleStyle(wb));

            String[] headers = {"姓名", "部门", "假别", "开始日期", "结束日期", "天数", "事由", "状态", "签字确认"};
            Row hr = ws.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle(wb));
            }
            int r = 2;
            for (LeaveApplication a : rows) {
                Row x = ws.createRow(r++);
                setCell(x, 0, a.getEmployeeName(), wb);
                setCell(x, 1, a.getDepartmentName() == null ? "" : a.getDepartmentName(), wb);
                setCell(x, 2, a.getLeaveTypeName(), wb);
                setCell(x, 3, asStr(a.getStartDate()), wb);
                setCell(x, 4, asStr(a.getEndDate()), wb);
                setCell(x, 5, a.getDays(), wb);
                setCell(x, 6, a.getReason() == null ? "" : a.getReason(), wb);
                setCell(x, 7, a.getStatus(), wb);
                setCell(x, 8, "", wb);
            }
            int[] widths = {12, 15, 12, 12, 12, 8, 30, 10, 15};
            for (int i = 0; i < widths.length; i++) ws.setColumnWidth(i, widths[i] * 512);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportLeaveApplications(int year, List<LeaveApplication> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet(year + "年请假记录");
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue(year + "年度请假记录明细表");
            title.setCellStyle(titleStyle(wb));

            String[] headers = {"ID", "姓名", "部门", "假别", "开始日期", "结束日期", "天数", "事由", "状态", "登记日期"};
            Row hr = ws.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headStyle(wb));
            }
            int r = 2;
            for (LeaveApplication a : rows) {
                Row x = ws.createRow(r++);
                x.createCell(0).setCellValue(a.getId());
                x.createCell(1).setCellValue(a.getEmployeeName() == null ? "" : a.getEmployeeName());
                x.createCell(2).setCellValue(a.getDepartmentName() == null ? "" : a.getDepartmentName());
                x.createCell(3).setCellValue(a.getLeaveTypeName() == null ? "" : a.getLeaveTypeName());
                x.createCell(4).setCellValue(a.getStartDate() == null ? "" : a.getStartDate().toString());
                x.createCell(5).setCellValue(a.getEndDate() == null ? "" : a.getEndDate().toString());
                x.createCell(6).setCellValue(a.getDays() == null ? 0 : a.getDays());
                x.createCell(7).setCellValue(a.getReason() == null ? "" : a.getReason());
                x.createCell(8).setCellValue(a.getStatus() == null ? "" : a.getStatus());
                x.createCell(9).setCellValue(a.getApplyDate() == null ? "" : a.getApplyDate().toString());
                for (int c = 0; c < 10; c++) {
                    x.getCell(c).setCellStyle(cellStyle(wb));
                }
            }
            int[] widths = {6, 10, 16, 12, 14, 14, 6, 28, 10, 14};
            for (int i = 0; i < widths.length; i++) ws.setColumnWidth(i, widths[i] * 512);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportSummary(int year, List<Map<String, Object>> rows) throws IOException {
        // Collect leave types in order
        java.util.LinkedHashSet<String> ltSet = new java.util.LinkedHashSet<>();
        for (Map<String, Object> r : rows) ltSet.add(asStr(r.get("leave_type")));
        java.util.List<String> ltList = new java.util.ArrayList<>(ltSet);

        // Pivot: key = deptName
        java.util.LinkedHashMap<String, Object[]> deptMap = new java.util.LinkedHashMap<>();
        java.util.List<String> deptOrder = new java.util.ArrayList<>();
        for (Map<String, Object> r : rows) {
            String dept = r.get("dept_name") == null ? "(未分配)" : asStr(r.get("dept_name"));
            if (!deptMap.containsKey(dept)) {
                deptMap.put(dept, new Object[]{new java.util.HashMap<String, double[]>(){{
                    for (String lt : ltList) put(lt, new double[]{0, 0});
                }}});
                deptOrder.add(dept);
            }
            String lt = asStr(r.get("leave_type"));
            double[] vals = ((java.util.Map<String, double[]>) deptMap.get(dept)[0]).get(lt);
            vals[0] += asDouble(r.get("total_days"));
            vals[1] += asLong(r.get("cnt"));
        }

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet ws = wb.createSheet(year + "年汇总");

            int totalCols = 1 + ltList.size() * 2 + 1;
            ws.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));
            Cell title = ws.createRow(0).createCell(0);
            title.setCellValue(year + "年度请假汇总表");
            title.setCellStyle(titleStyle(wb));

            // Row 1
            Row hr1 = ws.createRow(1);
            Cell c0 = hr1.createCell(0);
            c0.setCellValue("部门");
            c0.setCellStyle(headStyle(wb));
            ws.addMergedRegion(new CellRangeAddress(1, 2, 0, 0));

            int col = 1;
            for (String lt : ltList) {
                Cell clt = hr1.createCell(col);
                clt.setCellValue(lt);
                clt.setCellStyle(headStyle(wb));
                ws.addMergedRegion(new CellRangeAddress(1, 1, col, col + 1));
                col += 2;
            }

            Cell cTotal = hr1.createCell(col);
            cTotal.setCellValue("合计天数");
            cTotal.setCellStyle(headStyle(wb));
            ws.addMergedRegion(new CellRangeAddress(1, 2, col, col));

            // Row 2
            Row hr2 = ws.createRow(2);
            col = 1;
            for (int i = 0; i < ltList.size(); i++) {
                Cell cCnt = hr2.createCell(col);
                cCnt.setCellValue("人次");
                cCnt.setCellStyle(headStyle(wb));
                Cell cDays = hr2.createCell(col + 1);
                cDays.setCellValue("天数");
                cDays.setCellStyle(headStyle(wb));
                col += 2;
            }

            // Data rows
            int r = 3;
            for (String deptKey : deptOrder) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, double[]> ltData = (java.util.Map<String, double[]>) deptMap.get(deptKey)[0];
                Row x = ws.createRow(r++);
                setCell(x, 0, deptKey, wb);
                int c = 1;
                double totalDays = 0;
                for (String lt : ltList) {
                    double[] vals = ltData.get(lt);
                    setCell(x, c, vals[1] > 0 ? vals[1] : "", wb);
                    setCell(x, c + 1, vals[0] > 0 ? vals[0] : "", wb);
                    totalDays += vals[0];
                    c += 2;
                }
                setCell(x, c, totalDays, wb);
            }

            ws.setColumnWidth(0, 20 * 512);
            for (int i = 0; i < ltList.size(); i++) {
                ws.setColumnWidth(1 + i * 2, 8 * 512);
                ws.setColumnWidth(2 + i * 2, 10 * 512);
            }
            ws.setColumnWidth(totalCols - 1, 12 * 512);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==================== 样式工具 ====================

    private CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME);
        f.setFontHeightInPoints((short) 14);
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle headStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME);
        f.setFontHeightInPoints((short) 10);
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        thinBorder(s);
        return s;
    }

    private CellStyle cellStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName(FONT_NAME);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        thinBorder(s);
        return s;
    }

    private void thinBorder(CellStyle s) {
        BorderStyle b = BorderStyle.THIN;
        s.setBorderLeft(b);
        s.setBorderRight(b);
        s.setBorderTop(b);
        s.setBorderBottom(b);
    }

    private void setCell(Row row, int col, Object value, Workbook wb) {
        Cell c = row.createCell(col);
        if (value == null) {
            c.setCellValue("");
        } else if (value instanceof Number) {
            c.setCellValue(((Number) value).doubleValue());
        } else {
            c.setCellValue(asStr(value));
        }
        c.setCellStyle(cellStyle(wb));
    }

    private static String asStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private static double asDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }

    private static long asLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0; }
    }
}
