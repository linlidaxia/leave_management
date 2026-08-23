package com.leavemgmt.service;

import com.leavemgmt.model.Role;
import com.leavemgmt.model.User;
import com.leavemgmt.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理服务 (供管理员使用)
 */
@Service
public class UserService {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> findAll() {
        // 不返回 password_hash (安全考虑)
        List<User> list = userRepo.findAll();
        list.forEach(u -> u.setPasswordHash(null));
        return list;
    }

    public User findById(Long id) {
        User u = userRepo.findById(id).orElse(null);
        if (u != null) u.setPasswordHash(null);
        return u;
    }

    @Transactional
    public long createUser(String username, String rawPassword, String displayName, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalStateException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setDisplayName(displayName == null ? username : displayName);
        u.setRole(role == null ? Role.USER : role);
        u.setEnabled(true);
        return userRepo.save(u);
    }

    @Transactional
    public void changePassword(Long id, String newRawPassword) {
        if (newRawPassword == null || newRawPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        userRepo.updatePassword(id, encoder.encode(newRawPassword));
    }

    @Transactional
    public void updateProfile(Long id, String displayName, boolean enabled) {
        userRepo.updateProfile(id, displayName, enabled);
    }

    @Transactional
    public void updateRole(Long id, Role role) {
        userRepo.updateRole(id, role);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (id == 1L) {
            throw new IllegalStateException("不能删除默认管理员账号");
        }
        userRepo.delete(id);
    }

    /** 供 AuthenticationProvider 使用 */
    public User loadByUsername(String username) {
        return userRepo.findByUsername(username).orElse(null);
    }

    /** 校验明文密码与数据库哈希是否匹配 */
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
