package com.leavemgmt.repository;

import com.leavemgmt.model.LeaveCancellation;
import com.leavemgmt.util.SqliteDateUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

@Repository
public class LeaveCancellationRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<LeaveCancellation> MAPPER = (rs, rowNum) -> {
        LeaveCancellation c = new LeaveCancellation();
        c.setId(rs.getLong("id"));
        c.setApplicationId(rs.getLong("application_id"));
        c.setCancelDate(SqliteDateUtil.fromText(rs.getString("cancel_date")));
        c.setActualDays(rs.getDouble("actual_days"));
        c.setRemark(rs.getString("remark"));
        c.setEmployeeName(rs.getString("emp_name"));
        c.setDepartmentName(rs.getString("dept_name"));
        c.setLeaveTypeName(rs.getString("lt_name"));
        c.setStartDate(SqliteDateUtil.fromText(rs.getString("start_date")));
        c.setEndDate(SqliteDateUtil.fromText(rs.getString("end_date")));
        c.setLeaveDays(rs.getDouble("days"));
        return c;
    };

    public LeaveCancellationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LeaveCancellation> findAll() {
        String sql =
                "SELECT lc.id, lc.application_id, e.name AS emp_name, d.name AS dept_name, " +
                "lt.name AS lt_name, la.start_date, la.end_date, la.days, " +
                "lc.cancel_date, lc.actual_days, lc.remark " +
                "FROM leave_cancellations lc " +
                "INNER JOIN leave_applications la ON lc.application_id = la.id " +
                "INNER JOIN employees e ON la.employee_id = e.id " +
                "INNER JOIN leave_types lt ON la.leave_type_id = lt.id " +
                "LEFT JOIN departments d ON e.department_id = d.id " +
                "LEFT JOIN employee_identities ei ON e.identity_id = ei.id " +
                "ORDER BY lc.cancel_date DESC, lc.id DESC";
        return jdbc.query(sql, MAPPER);
    }

    public Long insert(Long applicationId, LocalDate cancelDate, double actualDays, String remark) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO leave_cancellations (application_id, cancel_date, actual_days, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, applicationId);
            ps.setString(2, SqliteDateUtil.toText(cancelDate));
            ps.setDouble(3, actualDays);
            ps.setString(4, remark);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int deleteByApplicationId(Long applicationId) {
        return jdbc.update("DELETE FROM leave_cancellations WHERE application_id=?", applicationId);
    }
}
