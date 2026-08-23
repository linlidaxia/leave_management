package com.leavemgmt.controller;

import com.leavemgmt.model.User;
import com.leavemgmt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserService userService;

    public AuthController(AuthenticationManager authManager, UserService userService) {
        this.authManager = authManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req,
                                      HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(req.username, req.password);
            Authentication auth = authManager.authenticate(token);

            HttpSession session = request.getSession(true);
            SecurityContext sc = SecurityContextHolder.getContext();
            sc.setAuthentication(auth);
            session.setAttribute("SPRING_SECURITY_CONTEXT", sc);

            return successResponse(auth);
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("success", false);
            m.put("message", "用户名或密码错误");
            return m;
        }
    }

    @PostMapping("/logout")
    public void logout() { /* handled by SecurityFilterChain */ }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal UserDetails principal) {
        Map<String, Object> m = new HashMap<>();
        if (principal == null) {
            m.put("authenticated", false);
            return m;
        }
        m.put("authenticated", true);
        m.put("username", principal.getUsername());
        String role = principal.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("USER");
        m.put("role", role);
        // 显示名
        User u = userService.loadByUsername(principal.getUsername());
        m.put("displayName", u == null ? principal.getUsername() : u.getDisplayName());
        return m;
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody ChangePasswordRequest req,
                                               @AuthenticationPrincipal UserDetails principal) {
        Map<String, Object> m = new HashMap<>();
        if (principal == null) {
            m.put("success", false);
            m.put("message", "未登录");
            return m;
        }
        try {
            // 找到当前用户
            User u = userService.loadByUsername(principal.getUsername());
            if (u == null) {
                m.put("success", false);
                m.put("message", "用户不存在");
                return m;
            }
            // 校验旧密码
            if (!userService.matches(req.oldPassword, u.getPasswordHash())) {
                m.put("success", false);
                m.put("message", "原密码不正确");
                return m;
            }
            userService.changePassword(u.getId(), req.newPassword);
            m.put("success", true);
            m.put("message", "密码已修改");
            return m;
        } catch (IllegalArgumentException e) {
            m.put("success", false);
            m.put("message", e.getMessage());
            return m;
        }
    }

    private Map<String, Object> successResponse(Authentication auth) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", true);
        m.put("username", auth.getName());
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("USER");
        m.put("role", role);
        return m;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class ChangePasswordRequest {
        public String oldPassword;
        public String newPassword;
    }
}
