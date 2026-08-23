package com.leavemgmt.controller;

import com.leavemgmt.model.Role;
import com.leavemgmt.model.User;
import com.leavemgmt.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理 REST 控制器
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Object list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        java.util.List<User> all = userService.findAll();
        if (page != null && size != null && size > 0) {
            int total = all.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            return new com.leavemgmt.model.PageResult<>(all.subList(fromIndex, toIndex), total, page, size);
        }
        return all;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody CreateUserRequest req) {
        Map<String, Object> m = new HashMap<>();
        try {
            long id = userService.createUser(req.username, req.password, req.displayName, Role.fromString(req.role));
            m.put("success", true);
            m.put("id", id);
        } catch (IllegalArgumentException | IllegalStateException e) {
            m.put("success", false);
            m.put("message", e.getMessage());
        }
        return m;
    }

    @PutMapping("/{id}/password")
    public Map<String, Object> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest req) {
        Map<String, Object> m = new HashMap<>();
        try {
            userService.changePassword(id, req.password);
            m.put("success", true);
        } catch (IllegalArgumentException e) {
            m.put("success", false);
            m.put("message", e.getMessage());
        }
        return m;
    }

    @PutMapping("/{id}/profile")
    public Map<String, Object> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(id, req.displayName, req.enabled);
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        return m;
    }

    @PutMapping("/{id}/role")
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest req) {
        userService.updateRole(id, Role.fromString(req.role));
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        return m;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> m = new HashMap<>();
        try {
            userService.deleteUser(id);
            m.put("success", true);
        } catch (IllegalStateException e) {
            m.put("success", false);
            m.put("message", e.getMessage());
        }
        return m;
    }

    public static class CreateUserRequest {
        public String username;
        public String password;
        public String displayName;
        public String role;
    }

    public static class ResetPasswordRequest {
        public String password;
    }

    public static class UpdateProfileRequest {
        public String displayName;
        public boolean enabled;
    }

    public static class UpdateRoleRequest {
        public String role;
    }
}
