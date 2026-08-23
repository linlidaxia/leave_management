package com.leavemgmt.config;

import com.leavemgmt.model.User;
import com.leavemgmt.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security UserDetailsService 实现: 从 SQLite users 表加载
 */
@Service
public class JdbcUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public JdbcUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userService.loadByUsername(username);
        if (u == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (!u.isEnabled()) {
            throw new UsernameNotFoundException("账号已禁用: " + username);
        }
        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .disabled(!u.isEnabled())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(u.getRole().authority())))
                .build();
    }
}
