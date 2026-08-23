package com.leavemgmt.repository;

import com.leavemgmt.model.DashboardStats;
import com.leavemgmt.model.LeaveApplication;
import com.leavemgmt.util.SqliteDateUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计 / 月度签字表 / 汇总表 / 首页统计查询
 */
@Repository
public class StatsRepository {

    private final JdbcTemplate jdbc;

    public StatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardStats getDashboardStats() {
        int year = LocalDate.now().getYear();
        DashboardStats s = new DashboardStats();
        Long n;
        n = jdbc.queryForObject("SELECT COUNT(*) FROM departments", Long.class);
        s.setDeptCount(n == null ? 0 : n);
        n = jdbc.queryForObject("SELECT COUNT(*) FROM employees", Long.class);
        s.setEmpCount(n == null ? 0 : n);
        n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM leave_applications WHERE CAST(strftime('%Y', start_date) AS INTEGER)=?",
                Long.class, year);
        s.setAppCount(n == null ? 0 : n);
        n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM leave_applications WHERE status='待审批' AND CAST(strftime('%Y', start_date) AS INTEGER)=?",
                Long.class, year);
        s.setPendingCount(n == null ? 0 : n);
        n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM leave_applications la WHERE la.status='已审批' AND " +
                "CAST(strftime('%Y', la.start_date) AS INTEGER)=? AND " +
                "la.id NOT IN (SELECT application_id FROM leave_cancellations)",
                Long.class, year);
        s.setCancelPending(n == null ? 0 : n);
        return s;
    }

    /**
     * 统计表: 按员工 + 假别 分组, 返回 [员工ID, 姓名, 部门, 假别, 总天数, 次数]
     */
    public List<Map<String, Object>> getStatistics(Integer year, Long deptId) {
        if (year == null) year = LocalDate.now().getYear();
        StringBuilder sql = new StringBuilder(
                "SELECT e.id AS emp_id, e.name AS emp_name, d.name AS dept_name, " +
                "lt.name AS leave_type, SUM(la.days) AS total_days, COUNT(la.id) AS cnt " +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "WHERE CAST(strftime('%Y', la.start_date) AS INTEGER)=?");
        List<Object> params = new ArrayList<>();
        params.add(year);
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        sql.append(" GROUP BY e.id, e.name, d.name, lt.name ORDER BY e.name, lt.name");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 月度签字表: 跨月份的请假记录 [姓名, 部门, 假别, 开始, 结束, 天数, 事由, 状态, 申请ID]
     */
    public List<LeaveApplication> getMonthlySignData(int year, int month, Long deptId) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = year == LocalDate.now().getYear() && month == 12
                ? LocalDate.of(year, 12, 31)
                : firstDay.plusMonths(1).minusDays(1);

        StringBuilder sql = new StringBuilder(
                "SELECT la.id, la.employee_id, e.name AS emp_name, d.name AS dept_name, " +
                "la.leave_type_id, lt.name AS lt_name, " +
                "la.start_date, la.start_period, la.end_date, la.end_period, " +
                "la.days, la.reason, la.status, la.apply_date, la.approver, la.approve_date, " +
                "la.offset_annual, la.remark " +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "WHERE la.start_date<=? AND la.end_date>=?");
        List<Object> params = new ArrayList<>();
        params.add(SqliteDateUtil.toText(lastDay));
        params.add(SqliteDateUtil.toText(firstDay));
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        sql.append(" ORDER BY e.name, la.start_date");

        RowMapper<LeaveApplication> mapper = (rs, rn) -> {
            LeaveApplication a = new LeaveApplication();
            a.setId(rs.getLong("id"));
            a.setEmployeeId(rs.getLong("employee_id"));
            a.setEmployeeName(rs.getString("emp_name"));
            a.setDepartmentName(rs.getString("dept_name"));
            a.setLeaveTypeId(rs.getLong("leave_type_id"));
            a.setLeaveTypeName(rs.getString("lt_name"));
            a.setStartDate(SqliteDateUtil.fromText(rs.getString("start_date")));
            a.setStartPeriod(rs.getString("start_period"));
            a.setEndDate(SqliteDateUtil.fromText(rs.getString("end_date")));
            a.setEndPeriod(rs.getString("end_period"));
            a.setDays(rs.getDouble("days"));
            a.setReason(rs.getString("reason"));
            a.setStatus(rs.getString("status"));
            a.setOffsetAnnual(rs.getDouble("offset_annual"));
            a.setReason(rs.getString("reason"));
            return a;
        };
        return jdbc.query(sql.toString(), mapper, params.toArray());
    }

    /**
     * 汇总表: 按部门 + 假别 分组 [部门, 假别, 总天数, 人次]
     */
    public List<Map<String, Object>> getSummaryData(Integer year, Long deptId) {
        if (year == null) year = LocalDate.now().getYear();
        StringBuilder sql = new StringBuilder(
                "SELECT d.name AS dept_name, lt.name AS leave_type, " +
                "SUM(la.days) AS total_days, COUNT(la.id) AS cnt " +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "WHERE CAST(strftime('%Y', la.start_date) AS INTEGER)=?");
        List<Object> params = new ArrayList<>();
        params.add(year);
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        sql.append(" GROUP BY d.name, lt.name ORDER BY d.name, lt.name");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }
}
