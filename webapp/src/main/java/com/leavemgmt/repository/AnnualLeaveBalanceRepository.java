package com.leavemgmt.repository;

import com.leavemgmt.model.AnnualLeaveBalance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnnualLeaveBalanceRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<AnnualLeaveBalance> MAPPER = (rs, rowNum) -> {
        AnnualLeaveBalance b = new AnnualLeaveBalance();
        b.setId(rs.getLong("id"));
        b.setEmployeeId(rs.getLong("employee_id"));
        b.setEmployeeName(rs.getString("emp_name"));
        b.setDepartmentName(rs.getString("dept_name"));
        b.setYearVal(rs.getInt("year_val"));
        b.setWorkYears(rs.getInt("work_years"));
        b.setTotalDays(rs.getDouble("total_days"));
        b.setUsedDays(rs.getDouble("used_days"));
        b.setRemainingDays(rs.getDouble("remaining_days"));
        b.setRemark(rs.getString("remark"));
        return b;
    };

    public AnnualLeaveBalanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AnnualLeaveBalance> findByYear(int year, Long deptId) {
        return findByYear(year, deptId, null);
    }

    public List<AnnualLeaveBalance> findByYear(int year, Long deptId, Long identityId) {
        StringBuilder sql = new StringBuilder(
                "SELECT alb.id, alb.employee_id, e.name AS emp_name, d.name AS dept_name, " +
                "alb.year_val, alb.work_years, alb.total_days, alb.used_days, alb.remaining_days, alb.remark " +
                "FROM annual_leave_balance alb " +
                "INNER JOIN employees e ON alb.employee_id = e.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id " +
                "WHERE alb.year_val=?");
        List<Object> params = new java.util.ArrayList<>();
        params.add(year);
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        if (identityId != null) {
            sql.append(" AND e.identity_id=?");
            params.add(identityId);
        }
        sql.append(" ORDER BY e.name");
        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    public AnnualLeaveBalance findByEmpYear(Long empId, int year) {
        List<AnnualLeaveBalance> list = jdbc.query(
                "SELECT alb.id, alb.employee_id, e.name AS emp_name, d.name AS dept_name, " +
                "alb.year_val, alb.work_years, alb.total_days, alb.used_days, alb.remaining_days, alb.remark " +
                "FROM annual_leave_balance alb " +
                "INNER JOIN employees e ON alb.employee_id = e.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "WHERE alb.employee_id=? AND alb.year_val=?",
                MAPPER, empId, year);
        return list.isEmpty() ? null : list.get(0);
    }

    public double getRemaining(Long empId, int year) {
        AnnualLeaveBalance b = findByEmpYear(empId, year);
        return b == null ? 0.0 : (b.getRemainingDays() == null ? 0.0 : b.getRemainingDays());
    }

    public int deleteByYear(int year) {
        return jdbc.update("DELETE FROM annual_leave_balance WHERE year_val=?", year);
    }

    public int deleteByEmployee(Long empId) {
        return jdbc.update("DELETE FROM annual_leave_balance WHERE employee_id=?", empId);
    }

    public int insert(Long empId, int year, int workYears, double total) {
        return jdbc.update(
                "INSERT INTO annual_leave_balance (employee_id, year_val, work_years, total_days, used_days, remaining_days, remark) " +
                "VALUES (?, ?, ?, ?, 0, ?, '')",
                empId, year, workYears, total, total);
    }

    public int addUsed(Long empId, int year, double usedDelta) {
        // 减少剩余, 增加已使用
        return jdbc.update(
                "UPDATE annual_leave_balance SET used_days = used_days + ?, remaining_days = remaining_days - ? " +
                "WHERE employee_id=? AND year_val=?",
                usedDelta, usedDelta, empId, year);
    }

    public int subtractUsed(Long empId, int year, double usedDelta) {
        return jdbc.update(
                "UPDATE annual_leave_balance SET used_days = used_days - ?, remaining_days = remaining_days + ? " +
                "WHERE employee_id=? AND year_val=?",
                usedDelta, usedDelta, empId, year);
    }

    public int updateTotals(Long id, double total, double used) {
        return jdbc.update(
                "UPDATE annual_leave_balance SET total_days=?, used_days=?, remaining_days=? WHERE id=?",
                total, used, total - used, id);
    }
}
