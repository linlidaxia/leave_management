package com.leavemgmt.repository;

import com.leavemgmt.model.LeaveType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class LeaveTypeRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<LeaveType> MAPPER = (rs, rowNum) -> {
        LeaveType lt = new LeaveType();
        lt.setId(rs.getLong("id"));
        lt.setName(rs.getString("name"));
        lt.setCode(rs.getString("code"));
        int so = rs.getInt("sort_order");
        lt.setSortOrder(rs.wasNull() ? null : so);
        int na = rs.getInt("need_attachment");
        lt.setNeedAttachment(!rs.wasNull() && na == 1);
        int da = rs.getInt("deduct_from_annual");
        lt.setDeductFromAnnual(!rs.wasNull() && da == 1);
        lt.setRemark(rs.getString("remark"));
        return lt;
    };

    public LeaveTypeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LeaveType> findAll() {
        return jdbc.query(
                "SELECT id, name, code, sort_order, need_attachment, deduct_from_annual, remark FROM leave_types ORDER BY sort_order, id",
                MAPPER);
    }

    public LeaveType findById(Long id) {
        List<LeaveType> list = jdbc.query(
                "SELECT id, name, code, sort_order, need_attachment, deduct_from_annual, remark FROM leave_types WHERE id=?",
                MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public LeaveType findByName(String name) {
        List<LeaveType> list = jdbc.query(
                "SELECT id, name, code, sort_order, need_attachment, deduct_from_annual, remark FROM leave_types WHERE name=?",
                MAPPER, name);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 检查假别是否已被请假记录引用 */
    public long countApplications(Long typeId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM leave_applications WHERE leave_type_id=?", Long.class, typeId);
        return n == null ? 0 : n;
    }

    public Long insert(LeaveType lt) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO leave_types (name, code, sort_order, need_attachment, deduct_from_annual, remark) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, lt.getName());
            ps.setString(2, lt.getCode());
            ps.setObject(3, lt.getSortOrder() == null ? 0 : lt.getSortOrder());
            ps.setInt(4, (lt.getNeedAttachment() != null && lt.getNeedAttachment()) ? 1 : 0);
            ps.setInt(5, (lt.getDeductFromAnnual() != null && lt.getDeductFromAnnual()) ? 1 : 0);
            ps.setString(6, lt.getRemark());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(LeaveType lt) {
        return jdbc.update(
                "UPDATE leave_types SET name=?, code=?, sort_order=?, need_attachment=?, deduct_from_annual=?, remark=? WHERE id=?",
                lt.getName(), lt.getCode(),
                lt.getSortOrder() == null ? 0 : lt.getSortOrder(),
                (lt.getNeedAttachment() != null && lt.getNeedAttachment()) ? 1 : 0,
                (lt.getDeductFromAnnual() != null && lt.getDeductFromAnnual()) ? 1 : 0,
                lt.getRemark(), lt.getId());
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM leave_types WHERE id=?", id);
    }
}
