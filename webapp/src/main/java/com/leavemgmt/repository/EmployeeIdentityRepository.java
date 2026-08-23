package com.leavemgmt.repository;

import com.leavemgmt.model.EmployeeIdentity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class EmployeeIdentityRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<EmployeeIdentity> MAPPER = (rs, rowNum) -> {
        EmployeeIdentity ei = new EmployeeIdentity();
        ei.setId(rs.getLong("id"));
        ei.setName(rs.getString("name"));
        ei.setCode(rs.getString("code"));
        ei.setSortOrder(rs.getInt("sort_order"));
        ei.setRemark(rs.getString("remark"));
        return ei;
    };

    public EmployeeIdentityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EmployeeIdentity> findAll() {
        return jdbc.query(
                "SELECT id, name, code, sort_order, remark FROM employee_identities ORDER BY sort_order, id",
                MAPPER);
    }

    public EmployeeIdentity findById(Long id) {
        List<EmployeeIdentity> list = jdbc.query(
                "SELECT id, name, code, sort_order, remark FROM employee_identities WHERE id=?",
                MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public EmployeeIdentity findByName(String name) {
        List<EmployeeIdentity> list = jdbc.query(
                "SELECT id, name, code, sort_order, remark FROM employee_identities WHERE name=?",
                MAPPER, name);
        return list.isEmpty() ? null : list.get(0);
    }

    public long countEmployees(Long identityId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE identity_id=?", Long.class, identityId);
        return n == null ? 0 : n;
    }

    public Long insert(EmployeeIdentity ei) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO employee_identities (name, code, sort_order, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ei.getName());
            ps.setString(2, ei.getCode());
            ps.setObject(3, ei.getSortOrder() == null ? 0 : ei.getSortOrder());
            ps.setString(4, ei.getRemark());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(EmployeeIdentity ei) {
        return jdbc.update(
                "UPDATE employee_identities SET name=?, code=?, sort_order=?, remark=? WHERE id=?",
                ei.getName(), ei.getCode(), ei.getSortOrder(), ei.getRemark(), ei.getId());
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM employee_identities WHERE id=?", id);
    }
}
