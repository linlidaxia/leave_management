package com.leavemgmt.repository;

import com.leavemgmt.model.Department;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class DepartmentRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Department> MAPPER = (rs, rowNum) -> {
        Department d = new Department();
        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        d.setCode(rs.getString("code"));
        int sort = rs.getInt("sort_order");
        d.setSortOrder(rs.wasNull() ? null : sort);
        d.setRemark(rs.getString("remark"));
        return d;
    };

    public DepartmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Department> findAll() {
        return jdbc.query("SELECT id, name, code, sort_order, remark FROM departments ORDER BY sort_order, id", MAPPER);
    }

    public Department findById(Long id) {
        List<Department> list = jdbc.query(
                "SELECT id, name, code, sort_order, remark FROM departments WHERE id=?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public long countEmployees(Long deptId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE department_id=?", Long.class, deptId);
        return n == null ? 0 : n;
    }

    public Long insert(Department d) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO departments (name, code, sort_order, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, d.getName());
            ps.setString(2, d.getCode());
            ps.setObject(3, d.getSortOrder() == null ? 0 : d.getSortOrder());
            ps.setString(4, d.getRemark());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Department d) {
        return jdbc.update(
                "UPDATE departments SET name=?, code=?, sort_order=?, remark=? WHERE id=?",
                d.getName(), d.getCode(),
                d.getSortOrder() == null ? 0 : d.getSortOrder(),
                d.getRemark(), d.getId());
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM departments WHERE id=?", id);
    }
}
