package com.leavemgmt.model;

/**
 * 用户角色枚举
 */
public enum Role {
    /** 管理员: 全权 + 用户管理 + 备份恢复 */
    ADMIN,
    /** 操作员: 部门/人员/请假/销假/公休假等日常操作 */
    USER,
    /** 查看员: 只读 */
    VIEWER;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Role fromString(String s) {
        if (s == null) return USER;
        try {
            return Role.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
