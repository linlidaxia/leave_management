package com.leavemgmt.repository;

import com.leavemgmt.model.User;
import com.leavemgmt.model.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<User> MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            Role.fromString(rs.getString("role")),
            rs.getInt("enabled") == 1
    );

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbc.query(
                "SELECT id, username, password_hash, display_name, role, enabled FROM users WHERE username=?",
                MAPPER, username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findById(Long id) {
        List<User> list = jdbc.query(
                "SELECT id, username, password_hash, display_name, role, enabled FROM users WHERE id=?",
                MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<User> findAll() {
        return jdbc.query(
                "SELECT id, username, password_hash, display_name, role, enabled FROM users ORDER BY id",
                MAPPER);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return n == null ? 0 : n;
    }

    public long save(User u) {
        return jdbc.update(
                "INSERT INTO users (username, password_hash, display_name, role, enabled) VALUES (?, ?, ?, ?, ?)",
                u.getUsername(), u.getPasswordHash(), u.getDisplayName(),
                u.getRole().name(), u.isEnabled() ? 1 : 0);
    }

    public long updatePassword(Long id, String passwordHash) {
        return jdbc.update("UPDATE users SET password_hash=? WHERE id=?", passwordHash, id);
    }

    public long updateProfile(Long id, String displayName, boolean enabled) {
        return jdbc.update("UPDATE users SET display_name=?, enabled=? WHERE id=?",
                displayName, enabled ? 1 : 0, id);
    }

    public long updateRole(Long id, Role role) {
        return jdbc.update("UPDATE users SET role=? WHERE id=?", role.name(), id);
    }

    public long delete(Long id) {
        return jdbc.update("DELETE FROM users WHERE id=?", id);
    }
}
