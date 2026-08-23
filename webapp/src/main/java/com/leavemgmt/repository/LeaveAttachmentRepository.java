package com.leavemgmt.repository;

import com.leavemgmt.model.LeaveAttachment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 请假附件仓储
 */
@Repository
public class LeaveAttachmentRepository {

    private final JdbcTemplate jdbc;

    /** 列表查询用的 Mapper (不含 file_data 大字段) */
    private static final RowMapper<LeaveAttachment> LIST_MAPPER = (rs, rowNum) -> {
        LeaveAttachment a = new LeaveAttachment();
        a.setId(rs.getLong("id"));
        a.setApplicationId(rs.getLong("application_id"));
        a.setFileName(rs.getString("file_name"));
        a.setFileSize(rs.getLong("file_size"));
        a.setContentType(rs.getString("content_type"));
        a.setUploadedAt(rs.getString("uploaded_at"));
        return a;
    };

    public LeaveAttachmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 列出某请假记录下的所有附件 (元信息, 不含二进制内容) */
    public List<LeaveAttachment> findByApplicationId(Long applicationId) {
        return jdbc.query(
                "SELECT id, application_id, file_name, file_size, content_type, uploaded_at " +
                "FROM leave_attachments WHERE application_id=? ORDER BY id",
                LIST_MAPPER, applicationId);
    }

    /** 获取附件完整信息 (含 file_data 二进制内容, 用于下载) */
    public LeaveAttachment findById(Long id) {
        List<LeaveAttachment> list = jdbc.query(
                "SELECT id, application_id, file_name, file_size, content_type, uploaded_at, file_data " +
                "FROM leave_attachments WHERE id=?",
                (rs, rowNum) -> {
                    LeaveAttachment a = new LeaveAttachment();
                    a.setId(rs.getLong("id"));
                    a.setApplicationId(rs.getLong("application_id"));
                    a.setFileName(rs.getString("file_name"));
                    a.setFileSize(rs.getLong("file_size"));
                    a.setContentType(rs.getString("content_type"));
                    a.setUploadedAt(rs.getString("uploaded_at"));
                    a.setFileData(rs.getBytes("file_data"));
                    return a;
                }, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(Long applicationId, String fileName, long fileSize,
                       String contentType, byte[] fileData) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO leave_attachments (application_id, file_name, file_size, content_type, file_data) " +
                    "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, applicationId);
            ps.setString(2, fileName);
            ps.setLong(3, fileSize);
            ps.setString(4, contentType);
            ps.setBytes(5, fileData);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM leave_attachments WHERE id=?", id);
    }

    public int deleteByApplicationId(Long applicationId) {
        return jdbc.update("DELETE FROM leave_attachments WHERE application_id=?", applicationId);
    }
}
