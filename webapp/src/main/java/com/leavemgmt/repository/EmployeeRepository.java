package com.leavemgmt.repository;

import com.leavemgmt.model.Employee;
import com.leavemgmt.util.SqliteDateUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Employee> MAPPER = (rs, rowNum) -> {
        Employee e = new Employee();
        e.setId(rs.getLong("id"));
        e.setName(rs.getString("name"));
        e.setGender(rs.getString("gender"));
        e.setIdCard(rs.getString("id_card"));
        long deptId = rs.getLong("department_id");
        e.setDepartmentId(rs.wasNull() ? null : deptId);
        e.setDepartmentName(rs.getString("dept_name"));
        e.setPosition(rs.getString("position"));
        e.setWorkStartDate(SqliteDateUtil.fromText(rs.getString("work_start_date")));
        e.setPhone(rs.getString("phone"));
        e.setRemark(rs.getString("remark"));
        return e;
    };

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Employee> findAll(Long deptId, String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT e.id, e.name, e.gender, e.id_card, e.department_id, " +
                "d.name AS dept_name, e.position, e.work_start_date, e.phone, e.remark " +
                "FROM employees e LEFT JOIN departments d ON e.department_id = d.id WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (deptId != null) {
            sql.append(" AND e.department_id=?");
            params.add(deptId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (e.name LIKE ? OR e.id_card LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY e.department_id, e.id");
        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    public Employee findById(Long id) {
        List<Employee> list = jdbc.query(
                "SELECT e.id, e.name, e.gender, e.id_card, e.department_id, " +
                "d.name AS dept_name, e.position, e.work_start_date, e.phone, e.remark " +
                "FROM employees e LEFT JOIN departments d ON e.department_id = d.id WHERE e.id=?",
                MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public long countApplications(Long empId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM leave_applications WHERE employee_id=?", Long.class, empId);
        return n == null ? 0 : n;
    }

    public List<Employee> findWorkInfoAll() {
        return jdbc.query(
                "SELECT id, NULL AS name, NULL AS gender, NULL AS id_card, department_id, NULL AS dept_name, NULL AS position, work_start_date, NULL AS phone, NULL AS remark FROM employees",
                (rs, rn) -> {
                    Employee e = new Employee();
                    e.setId(rs.getLong("id"));
                    e.setWorkStartDate(SqliteDateUtil.fromText(rs.getString("work_start_date")));
                    return e;
                });
    }

    public Long insert(Employee e) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO employees (name, gender, id_card, department_id, position, work_start_date, phone, remark) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.getName());
            ps.setString(2, e.getGender());
            ps.setString(3, e.getIdCard());
            ps.setObject(4, e.getDepartmentId());
            ps.setString(5, e.getPosition());
            ps.setString(6, SqliteDateUtil.toText(e.getWorkStartDate()));
            ps.setString(7, e.getPhone());
            ps.setString(8, e.getRemark());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Employee e) {
        return jdbc.update(
                "UPDATE employees SET name=?, gender=?, id_card=?, department_id=?, position=?, work_start_date=?, phone=?, remark=? WHERE id=?",
                e.getName(), e.getGender(), e.getIdCard(), e.getDepartmentId(), e.getPosition(),
                SqliteDateUtil.toText(e.getWorkStartDate()), e.getPhone(), e.getRemark(), e.getId());
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM employees WHERE id=?", id);
    }
}
