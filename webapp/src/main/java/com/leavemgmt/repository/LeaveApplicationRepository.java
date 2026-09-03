package com.leavemgmt.repository;

import com.leavemgmt.model.LeaveApplication;
import com.leavemgmt.util.SqliteDateUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class LeaveApplicationRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<LeaveApplication> MAPPER = (rs, rowNum) -> {
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
        a.setApplyDate(SqliteDateUtil.fromText(rs.getString("apply_date")));
        a.setApprover(rs.getString("approver"));
        a.setApproveDate(SqliteDateUtil.fromText(rs.getString("approve_date")));
        a.setOffsetAnnual(rs.getDouble("offset_annual"));
        a.setRemark(rs.getString("remark"));
        return a;
    };

    public LeaveApplicationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLS =
            "la.id, la.employee_id, e.name AS emp_name, d.name AS dept_name, " +
            "e.identity_id, ei.name AS identity_name, " +
            "la.leave_type_id, lt.name AS lt_name, " +
            "la.start_date, la.start_period, la.end_date, la.end_period, " +
            "la.days, la.reason, la.status, la.apply_date, la.approver, la.approve_date, " +
            "la.offset_annual, la.remark ";

    public List<LeaveApplication> findAll(Integer year, Long deptId, String status) {
        return findAll(year, deptId, null, null, status, null, null, null);
    }

    public List<LeaveApplication> findAll(Integer year, Long deptId, Long employeeId,
            Long leaveTypeId, String status, Long identityId, String startDate, String endDate) {
        return findAll(year, deptId, employeeId, leaveTypeId, status, identityId, startDate, endDate, null);
    }

    public List<LeaveApplication> findAll(Integer year, Long deptId, Long employeeId,
            Long leaveTypeId, String status, Long identityId, String startDate, String endDate,
            Boolean annualRelated) {
        StringBuilder sql = new StringBuilder(
                "SELECT " + SELECT_COLS +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (year != null) {
            sql.append(" AND CAST(strftime('%Y', la.start_date) AS INTEGER)=?");
            params.add(year);
        }
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        if (employeeId != null) {
            sql.append(" AND la.employee_id=?");
            params.add(employeeId);
        }
        if (leaveTypeId != null) {
            sql.append(" AND la.leave_type_id=?");
            params.add(leaveTypeId);
        }
        if (identityId != null) {
            sql.append(" AND e.identity_id=?");
            params.add(identityId);
        }
        if (annualRelated != null && annualRelated) {
            // 仅统计与年假相关的请假: 公休假, 以及抵扣了年假的事假
            sql.append(" AND ((lt.deduct_from_annual = 1 AND la.offset_annual > 0) " +
                    "OR lt.name LIKE '%公休假%' OR lt.name LIKE '%年休假%')");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND la.status=?");
            params.add(status);
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND la.start_date>=?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND la.end_date<=?");
            params.add(endDate);
        }
        sql.append(" ORDER BY la.start_date DESC, la.id DESC");
        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    public LeaveApplication findById(Long id) {
        List<LeaveApplication> list = jdbc.query(
                "SELECT " + SELECT_COLS +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id WHERE la.id=?",
                MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(LeaveApplication a) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO leave_applications " +
                    "(employee_id, leave_type_id, start_date, start_period, end_date, end_period, " +
                    " days, reason, status, apply_date, offset_annual, remark) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.getEmployeeId());
            ps.setLong(2, a.getLeaveTypeId());
            ps.setString(3, SqliteDateUtil.toText(a.getStartDate()));
            ps.setString(4, a.getStartPeriod());
            ps.setString(5, SqliteDateUtil.toText(a.getEndDate()));
            ps.setString(6, a.getEndPeriod());
            ps.setDouble(7, a.getDays());
            ps.setString(8, a.getReason());
            ps.setString(9, a.getStatus() == null ? "待审批" : a.getStatus());
            ps.setString(10, SqliteDateUtil.toText(LocalDate.now()));
            ps.setObject(11, a.getOffsetAnnual() == null ? 0 : a.getOffsetAnnual());
            ps.setString(12, a.getRemark());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateStatus(Long id, String status, String approver) {
        return jdbc.update(
                "UPDATE leave_applications SET status=?, approver=?, approve_date=? WHERE id=?",
                status, approver, SqliteDateUtil.toText(LocalDate.now()), id);
    }

    public int updateOffsetAnnual(Long id, double offsetAnnual) {
        return jdbc.update(
                "UPDATE leave_applications SET offset_annual=? WHERE id=?",
                offsetAnnual, id);
    }

    public int updateApproveDate(Long id, LocalDate approveDate) {
        return jdbc.update(
                "UPDATE leave_applications SET approve_date=? WHERE id=?",
                SqliteDateUtil.toText(approveDate), id);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM leave_applications WHERE id=?", id);
    }

    public List<LeaveApplication> findPendingCancellations() {
        String sql =
                "SELECT " + SELECT_COLS +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id " +
                "WHERE la.status='已审批' AND la.id NOT IN (SELECT application_id FROM leave_cancellations) " +
                "ORDER BY la.start_date DESC";
        return jdbc.query(sql, MAPPER);
    }

    /** 待销假记录多条件查询 */
    public List<LeaveApplication> findPendingCancellationsFiltered(
            Long deptId, Long employeeId, Long leaveTypeId,
            String startDate, String endDate) {
        return findPendingCancellationsFiltered(deptId, employeeId, leaveTypeId, startDate, endDate, null);
    }

    public List<LeaveApplication> findPendingCancellationsFiltered(
            Long deptId, Long employeeId, Long leaveTypeId,
            String startDate, String endDate, Long identityId) {
        return findPendingCancellationsFiltered(deptId, employeeId, leaveTypeId, startDate, endDate, identityId, null);
    }

    public List<LeaveApplication> findPendingCancellationsFiltered(
            Long deptId, Long employeeId, Long leaveTypeId,
            String startDate, String endDate, Long identityId, Integer year) {
        StringBuilder sql = new StringBuilder(
                "SELECT " + SELECT_COLS +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id " +
                "WHERE la.status='已审批' AND la.id NOT IN (SELECT application_id FROM leave_cancellations)");
        List<Object> params = new ArrayList<>();
        if (year != null) {
            sql.append(" AND CAST(strftime('%Y', la.start_date) AS INTEGER)=?");
            params.add(year);
        }
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        if (employeeId != null) {
            sql.append(" AND la.employee_id=?");
            params.add(employeeId);
        }
        if (leaveTypeId != null) {
            sql.append(" AND la.leave_type_id=?");
            params.add(leaveTypeId);
        }
        if (identityId != null) {
            sql.append(" AND e.identity_id=?");
            params.add(identityId);
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND la.start_date>=?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND la.end_date<=?");
            params.add(endDate);
        }
        sql.append(" ORDER BY la.start_date DESC");
        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    /** 找出本年度涉及事假抵扣公休假的申请 (用于初始化额度时恢复已使用天数) */
    public List<LeaveApplication> findWithOffset(int year) {
        String sql =
                "SELECT " + SELECT_COLS +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id " +
                "WHERE la.offset_annual > 0 AND CAST(strftime('%Y', la.start_date) AS INTEGER)=?";
        return jdbc.query(sql, MAPPER, year);
    }

    /**
     * 查找与指定员工和日期范围重叠的请假记录 (排除已销假)
     * 重叠条件: newStart <= existingEnd AND existingStart <= newEnd
     * @param excludeId 排除的申请ID (编辑时排除自身)
     */
    public List<LeaveApplication> findOverlapping(Long employeeId, String startDate, String endDate, Long excludeId) {
        StringBuilder sql = new StringBuilder(
                "SELECT " + SELECT_COLS +
                "FROM leave_applications la " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id " +
                "WHERE la.employee_id=? AND la.status != '已销假' " +
                "AND la.start_date <= ? AND la.end_date >= ?");
        List<Object> params = new ArrayList<>();
        params.add(employeeId);
        params.add(endDate);
        params.add(startDate);
        if (excludeId != null) {
            sql.append(" AND la.id != ?");
            params.add(excludeId);
        }
        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }
}
