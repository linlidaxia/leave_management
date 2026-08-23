package com.leavemgmt.util;

import java.time.LocalDate;

/**
 * 日期工具: SQLite 中所有日期字段统一按字符串 (TEXT) 存储,
 * 通过此工具类完成 LocalDate ↔ String 转换.
 *
 * <p>这样可避免 SQLite JDBC 驱动将 setDate() 写入为 long 时间戳,
 * 而读取时又用严格格式解析导致的 "Unparseable date" 错误.
 */
public final class SqliteDateUtil {

    private SqliteDateUtil() {}

    /** 将 LocalDate 转换为 SQLite 中存储的字符串形式 (yyyy-MM-dd) */
    public static String toText(LocalDate date) {
        return date == null ? null : date.toString();
    }

    /**
     * 将 SQLite 中读取的日期值转换为 LocalDate.
     * 兼容多种可能格式:
     * - "yyyy-MM-dd" (推荐, 文本存储)
     * - "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd HH:mm:ss.SSS" (datetime 字符串)
     * - 纯数字 long 毫秒时间戳 (旧驱动写入)
     * - null
     */
    public static LocalDate fromText(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        // 兼容 long 毫秒时间戳 (例如 "1268582400000")
        if (s.matches("\\d{10,}")) {
            try {
                long ms = Long.parseLong(s);
                return new java.util.Date(ms).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (NumberFormatException ignored) { }
        }
        // 取前 10 位作为 yyyy-MM-dd
        if (s.length() >= 10) {
            try {
                return LocalDate.parse(s.substring(0, 10));
            } catch (Exception ignored) { }
        }
        return null;
    }
}
