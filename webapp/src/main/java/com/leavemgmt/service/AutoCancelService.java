package com.leavemgmt.service;

import com.leavemgmt.model.LeaveApplication;
import com.leavemgmt.repository.LeaveApplicationRepository;
import com.leavemgmt.repository.LeaveCancellationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 自动销假服务
 * 定时检查已审批但未销假的请假记录, 如果当前时间已超过请假结束时间, 自动执行销假
 *
 * 调度策略:
 * - 每天 01:00: 销假 endDate < today 的记录 (昨天及之前结束的)
 * - 每天 13:00: 销假 endDate = today 且 endPeriod = "上午" 的记录 (今天上午结束的)
 */
@Service
public class AutoCancelService {

    private static final Logger log = LoggerFactory.getLogger(AutoCancelService.class);

    private final LeaveApplicationRepository appRepo;
    private final LeaveCancellationRepository cancelRepo;

    public AutoCancelService(LeaveApplicationRepository appRepo,
                             LeaveCancellationRepository cancelRepo) {
        this.appRepo = appRepo;
        this.cancelRepo = cancelRepo;
    }

    /**
     * 每天 01:00 执行: 销假结束日期在昨天及之前的记录
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cancelExpiredBeforeToday() {
        LocalDate today = LocalDate.now();
        log.info("[自动销假] 01:00 执行, 检查结束日期 < {} 的记录", today);
        int count = doAutoCancel(today, false);
        log.info("[自动销假] 完成, 共处理 {} 条记录", count);
    }

    /**
     * 每天 13:00 执行: 销假结束日期 = 今天 且 结束时段 = "上午" 的记录
     */
    @Scheduled(cron = "0 0 13 * * ?")
    @Transactional
    public void cancelExpiredMorningToday() {
        LocalDate today = LocalDate.now();
        log.info("[自动销假] 13:00 执行, 检查结束日期={} 且时段=上午的记录", today);
        int count = doAutoCancel(today, true);
        log.info("[自动销假] 完成, 共处理 {} 条记录", count);
    }

    /**
     * 执行自动销假
     * @param today 今天的日期
     * @param sameDayOnly true=只处理 endDate=today 的, false=处理 endDate < today 的
     */
    private int doAutoCancel(LocalDate today, boolean sameDayOnly) {
        // 查询所有已审批但未销假的记录
        List<LeaveApplication> pending = appRepo.findPendingCancellations();
        int count = 0;

        for (LeaveApplication app : pending) {
            if (shouldCancel(app, today, sameDayOnly)) {
                try {
                    cancelOne(app, today);
                    count++;
                } catch (Exception e) {
                    log.error("[自动销假] 处理请假记录 #{} 失败: {}", app.getId(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * 判断该请假记录是否应该被自动销假
     */
    private boolean shouldCancel(LeaveApplication app, LocalDate today, boolean sameDayOnly) {
        LocalDate endDate = app.getEndDate();
        if (endDate == null) return false;

        String endPeriod = app.getEndPeriod();

        if (sameDayOnly) {
            // 13:00 任务: 只处理 endDate = today 且 endPeriod = "上午" 的
            return endDate.equals(today) && "上午".equals(endPeriod);
        } else {
            // 01:00 任务: 处理 endDate < today 的 (所有时段)
            // 以及 endDate = today 且 endPeriod = "下午" 的 (昨天下午结束, 今天凌晨也该销假)
            if (endDate.isBefore(today)) return true;
            if (endDate.equals(today) && "下午".equals(endPeriod)) return true;
            return false;
        }
    }

    /**
     * 对单条请假记录执行销假
     */
    private void cancelOne(LeaveApplication app, LocalDate cancelDate) {
        double actualDays = app.getDays() != null ? app.getDays() : 0;

        // 1. 创建销假记录
        cancelRepo.insert(app.getId(), cancelDate, actualDays, "系统自动销假");

        // 2. 更新请假记录状态为 "已销假"
        appRepo.updateStatus(app.getId(), "已销假", null);

        log.info("[自动销假] 已销假: 请假记录 #{} ({} {} ~ {} {}), {}天",
                app.getId(),
                app.getStartDate(), app.getStartPeriod(),
                app.getEndDate(), app.getEndPeriod(),
                actualDays);
    }
}
