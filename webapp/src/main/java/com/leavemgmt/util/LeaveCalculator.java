package com.leavemgmt.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 请销假业务计算工具类
 *
 * <p>对应原 Python 项目 db_manager.py 中的三个核心函数:
 * <ul>
 *   <li>calculate_work_years - 工龄计算</li>
 *   <li>get_annual_leave_days - 公休假天数</li>
 *   <li>calculate_leave_days - 请假天数 (支持半天)</li>
 * </ul>
 */
public final class LeaveCalculator {

    public static final String PERIOD_AM = "上午";
    public static final String PERIOD_PM = "下午";

    private LeaveCalculator() {}

    /**
     * 计算工龄 (满年). 与原版一致:
     * 若当前月日早于参加工作月日, 则少算 1 年.
     */
    public static int calculateWorkYears(LocalDate workStart, LocalDate refDate) {
        if (workStart == null) return 0;
        if (refDate == null) refDate = LocalDate.now();
        long years = ChronoUnit.YEARS.between(workStart, refDate);
        // Adjust for not-yet-reached anniversary this year
        if (refDate.getDayOfYear() < workStart.getDayOfYear()) {
            years--;
        }
        return (int) Math.max(years, 0);
    }

    public static int calculateWorkYears(LocalDate workStart) {
        return calculateWorkYears(workStart, LocalDate.now());
    }

    /**
     * 根据工龄获取公休假天数:
     * <ul>
     *   <li>工龄 &lt; 10 年: 5 天</li>
     *   <li>10 ≤ 工龄 &lt; 20 年: 10 天</li>
     *   <li>工龄 ≥ 20 年: 15 天</li>
     * </ul>
     */
    public static double getAnnualLeaveDays(int workYears) {
        if (workYears < 10) return 5.0;
        if (workYears < 20) return 10.0;
        return 15.0;
    }

    /**
     * 计算请假天数, 支持上午/下午, 半天 = 0.5 天.
     *
     * 规则:
     * - 同日: 同时段 0.5 天; 跨时段 1 天
     * - 跨日: (结束 - 开始 + 1) - (起始为下午 -0.5) - (结束为上午 -0.5)
     */
    public static double calculateLeaveDays(LocalDate start, String startPeriod,
                                           LocalDate end, String endPeriod) {
        if (start == null || end == null) return 0;
        if (start.equals(end)) {
            return startPeriod != null && startPeriod.equals(endPeriod) ? 0.5 : 1.0;
        }
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (PERIOD_PM.equals(startPeriod)) totalDays -= 0.5;
        if (PERIOD_AM.equals(endPeriod))   totalDays -= 0.5;
        return Math.max(totalDays, 0.5);
    }

    /**
     * 判断假别名称是否属于"事假" (用于自动抵扣公休假).
     * @deprecated 请使用 LeaveType.getDeductFromAnnual() 替代
     */
    @Deprecated
    public static boolean isPersonalLeave(String leaveTypeName) {
        return leaveTypeName != null && leaveTypeName.contains("事假");
    }

    /**
     * 判断假别名称是否属于"公休假" (用于额度校验).
     */
    public static boolean isAnnualLeave(String leaveTypeName) {
        return leaveTypeName != null && (leaveTypeName.contains("公休假") || leaveTypeName.contains("年休假"));
    }

    /**
     * 判断假别是否需要优先扣除公休假.
     * 基于 LeaveType.deductFromAnnual 字段判断.
     */
    public static boolean isDeductFromAnnual(com.leavemgmt.model.LeaveType leaveType) {
        return leaveType != null && Boolean.TRUE.equals(leaveType.getDeductFromAnnual());
    }

    /**
     * 计算实际从公休假扣除的天数.
     * @param totalDays 请假总天数
     * @param annualRemaining 公休假剩余额度
     * @return 实际扣除天数 (不超过剩余额度)
     */
    public static double calculateAnnualDeduction(double totalDays, double annualRemaining) {
        if (annualRemaining <= 0) return 0;
        return Math.min(totalDays, annualRemaining);
    }
}
