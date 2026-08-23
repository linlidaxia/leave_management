package com.leavemgmt.service;

import com.leavemgmt.model.AnnualLeaveBalance;
import com.leavemgmt.model.DashboardStats;
import com.leavemgmt.model.Department;
import com.leavemgmt.model.Employee;
import com.leavemgmt.model.LeaveApplication;
import com.leavemgmt.model.LeaveCancellation;
import com.leavemgmt.model.LeaveType;
import com.leavemgmt.repository.AnnualLeaveBalanceRepository;
import com.leavemgmt.repository.DepartmentRepository;
import com.leavemgmt.repository.EmployeeRepository;
import com.leavemgmt.repository.LeaveApplicationRepository;
import com.leavemgmt.repository.LeaveAttachmentRepository;
import com.leavemgmt.repository.LeaveCancellationRepository;
import com.leavemgmt.repository.LeaveTypeRepository;
import com.leavemgmt.repository.StatsRepository;
import com.leavemgmt.util.LeaveCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请销假核心业务服务
 *
 * <p>完整复刻原 Python 项目 db_manager.py 的全部业务规则:
 * <ul>
 *   <li>工龄计算 (满年扣减)</li>
 *   <li>公休假天数规则 (<10y=5, 10-20y=10, &gt;=20y=15)</li>
 *   <li>请假天数计算 (半天=0.5)</li>
 *   <li>事假自动抵扣公休假 (抵扣额不超过余额)</li>
 *   <li>删除请假记录时恢复抵扣天数</li>
 *   <li>销假登记</li>
 *   <li>年度公休假额度初始化 (按工龄 + 重放本年事假抵扣)</li>
 * </ul>
 */
@Service
public class LeaveService {

    private final DepartmentRepository deptRepo;
    private final EmployeeRepository empRepo;
    private final LeaveTypeRepository ltRepo;
    private final LeaveApplicationRepository appRepo;
    private final LeaveCancellationRepository cancelRepo;
    private final AnnualLeaveBalanceRepository balRepo;
    private final StatsRepository statsRepo;
    private final LeaveAttachmentRepository attachRepo;

    public LeaveService(DepartmentRepository deptRepo,
                        EmployeeRepository empRepo,
                        LeaveTypeRepository ltRepo,
                        LeaveApplicationRepository appRepo,
                        LeaveCancellationRepository cancelRepo,
                        AnnualLeaveBalanceRepository balRepo,
                        StatsRepository statsRepo,
                        LeaveAttachmentRepository attachRepo) {
        this.deptRepo = deptRepo;
        this.empRepo = empRepo;
        this.ltRepo = ltRepo;
        this.appRepo = appRepo;
        this.cancelRepo = cancelRepo;
        this.balRepo = balRepo;
        this.statsRepo = statsRepo;
        this.attachRepo = attachRepo;
    }

    // ==================== 首页概览 ====================

    public DashboardStats getDashboardStats() {
        return statsRepo.getDashboardStats();
    }

    // ==================== 部门管理 ====================

    public List<Department> listDepartments() {
        return deptRepo.findAll();
    }

    @Transactional
    public Long saveDepartment(Department d) {
        if (d.getId() == null) {
            return deptRepo.insert(d);
        }
        deptRepo.update(d);
        return d.getId();
    }

    @Transactional
    public void deleteDepartment(Long id) {
        long count = deptRepo.countEmployees(id);
        if (count > 0) {
            throw new IllegalStateException("该部门下还有 " + count + " 名员工，无法删除");
        }
        deptRepo.delete(id);
    }

    // ==================== 人员管理 ====================

    public List<Employee> listEmployees(Long deptId, String keyword) {
        return empRepo.findAll(deptId, keyword);
    }

    public Employee getEmployee(Long id) {
        return empRepo.findById(id);
    }

    /** 计算员工工龄 (基于 work_start_date) */
    public int getWorkYears(Employee e) {
        if (e == null || e.getWorkStartDate() == null) return 0;
        return LeaveCalculator.calculateWorkYears(e.getWorkStartDate());
    }

    /** 计算员工公休假天数 */
    public double getAnnualLeaveDays(Employee e) {
        return LeaveCalculator.getAnnualLeaveDays(getWorkYears(e));
    }

    /** 员工 + 工龄 + 公休假天数 (用于列表展示) */
    public List<Map<String, Object>> listEmployeesWithLeave(Long deptId, String keyword) {
        List<Employee> emps = empRepo.findAll(deptId, keyword);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        int currentYear = java.time.LocalDate.now().getYear();
        for (Employee e : emps) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("name", e.getName());
            m.put("gender", e.getGender());
            m.put("idCard", e.getIdCard());
            m.put("departmentId", e.getDepartmentId());
            m.put("departmentName", e.getDepartmentName());
            m.put("position", e.getPosition());
            m.put("workStartDate", e.getWorkStartDate());
            m.put("workYears", getWorkYears(e));
            // 按规则计算的年度额度
            double annualTotal = getAnnualLeaveDays(e);
            m.put("annualDays", annualTotal);
            m.put("phone", e.getPhone());
            m.put("remark", e.getRemark());

            // 查询实际公休假余额记录 (如果已初始化)
            AnnualLeaveBalance bal = balRepo.findByEmpYear(e.getId(), currentYear);
            if (bal != null) {
                m.put("annualTotalActual", bal.getTotalDays() == null ? annualTotal : bal.getTotalDays());
                m.put("annualUsed", bal.getUsedDays() == null ? 0.0 : bal.getUsedDays());
                m.put("annualRemaining", bal.getRemainingDays() == null ? annualTotal : bal.getRemainingDays());
                m.put("annualBalanceInitialized", true);
            } else {
                // 未初始化: 显示按规则计算值, 已用=0
                m.put("annualTotalActual", annualTotal);
                m.put("annualUsed", 0.0);
                m.put("annualRemaining", annualTotal);
                m.put("annualBalanceInitialized", false);
            }
            result.add(m);
        }
        return result;
    }

    @Transactional
    public Long saveEmployee(Employee e) {
        if (e.getId() == null) {
            return empRepo.insert(e);
        }
        empRepo.update(e);
        return e.getId();
    }

    @Transactional
    public void deleteEmployee(Long id) {
        long count = empRepo.countApplications(id);
        if (count > 0) {
            throw new IllegalStateException("该员工有 " + count + " 条请假记录，无法删除");
        }
        // 删除公休假额度 (SQLite 无 ON DELETE CASCADE, 需手动处理)
        balRepo.deleteByEmployee(id);
        empRepo.delete(id);
    }

    // ==================== 假别管理 ====================

    public List<LeaveType> listLeaveTypes() {
        return ltRepo.findAll();
    }

    @Transactional
    public Long saveLeaveType(LeaveType lt) {
        // 名称唯一性校验
        LeaveType existing = ltRepo.findByName(lt.getName());
        if (existing != null && !existing.getId().equals(lt.getId())) {
            throw new IllegalStateException("假别名称「" + lt.getName() + "」已存在");
        }
        if (lt.getId() == null) {
            return ltRepo.insert(lt);
        }
        ltRepo.update(lt);
        return lt.getId();
    }

    @Transactional
    public void deleteLeaveType(Long id) {
        // 保护系统内置假别 (公休假 ID=1, 事假 ID=2 — 这两个是事假抵扣公休假业务逻辑的关键)
        if (id != null && (id == 1L || id == 2L)) {
            String name = ltRepo.findById(id) != null ? ltRepo.findById(id).getName() : String.valueOf(id);
            throw new IllegalStateException("假别「" + name + "」为系统内置，不可删除");
        }
        long count = ltRepo.countApplications(id);
        if (count > 0) {
            throw new IllegalStateException("该假别有 " + count + " 条请假记录，无法删除");
        }
        ltRepo.delete(id);
    }

    // ==================== 请假申请 ====================

    public List<LeaveApplication> listApplications(Integer year, Long deptId, String status) {
        return appRepo.findAll(year, deptId, status);
    }

    public LeaveApplication getApplication(Long id) {
        return appRepo.findById(id);
    }

    /**
     * 计算请假天数 (用于前端预览)
     */
    public double calcLeaveDays(LocalDate start, String startPeriod,
                               LocalDate end, String endPeriod) {
        return LeaveCalculator.calculateLeaveDays(start, startPeriod, end, endPeriod);
    }

    /**
     * 检查事假抵扣公休假的预览信息
     * 返回: { days, offsetAnnual, remaining, message, leaveCategory, annualTotal, annualUsed }
     */
    public Map<String, Object> previewApplication(Long empId, Long leaveTypeId,
                                                  LocalDate start, String startPeriod,
                                                  LocalDate end, String endPeriod) {
        double days = LeaveCalculator.calculateLeaveDays(start, startPeriod, end, endPeriod);
        LeaveType lt = ltRepo.findById(leaveTypeId);
        Map<String, Object> r = new HashMap<>();
        r.put("days", days);
        r.put("leaveTypeName", lt == null ? "" : lt.getName());
        double offset = 0;
        String msg = "";
        String category = "other";  // other / annual / personal

        int year = start.getYear();
        AnnualLeaveBalance bal = balRepo.findByEmpYear(empId, year);
        double annualTotal = bal == null ? 0 : (bal.getTotalDays() == null ? 0 : bal.getTotalDays());
        double annualUsed = bal == null ? 0 : (bal.getUsedDays() == null ? 0 : bal.getUsedDays());
        double annualRemaining = bal == null ? 0 : (bal.getRemainingDays() == null ? 0 : bal.getRemainingDays());

        if (lt != null && LeaveCalculator.isAnnualLeave(lt.getName())) {
            // 公休假: 校验不能超过剩余额度
            category = "annual";
            r.put("exceeds", days > annualRemaining);
            r.put("annualTotal", annualTotal);
            r.put("annualUsed", annualUsed);
            r.put("annualRemaining", annualRemaining);
            if (days > annualRemaining) {
                msg = String.format("⚠ 公休假超额: 本次申请 %.1f 天, 剩余额度仅 %.1f 天 (超 %.1f 天)", days, annualRemaining, days - annualRemaining);
            } else {
                msg = String.format("公休假剩余 %.1f 天, 本次申请 %.1f 天, 申请后剩余 %.1f 天",
                        annualRemaining, days, annualRemaining - days);
            }
        } else if (lt != null && LeaveCalculator.isPersonalLeave(lt.getName())) {
            // 事假: 自动抵扣公休假
            category = "personal";
            offset = Math.min(days, annualRemaining);
            if (offset > 0) {
                msg = String.format("事假将抵扣公休假 %.1f 天 (公休假剩余 %.1f 天)", offset, annualRemaining);
            } else {
                msg = "公休假已无余额, 不抵扣";
            }
        }
        r.put("leaveCategory", category);
        r.put("offsetAnnual", offset);
        r.put("remaining", annualRemaining);
        r.put("annualTotal", annualTotal);
        r.put("annualUsed", annualUsed);
        r.put("annualRemaining", annualRemaining);
        r.put("message", msg);
        return r;
    }

    /**
     * 新建请假申请 (含事假自动抵扣公休假 / 公休假额度校验)
     */
    @Transactional
    public Long submitApplication(Long empId, Long leaveTypeId,
                                  LocalDate start, String startPeriod,
                                  LocalDate end, String endPeriod,
                                  String reason) {
        double days = LeaveCalculator.calculateLeaveDays(start, startPeriod, end, endPeriod);
        LeaveType lt = ltRepo.findById(leaveTypeId);
        int year = start.getYear();

        // 公休假额度校验
        if (lt != null && LeaveCalculator.isAnnualLeave(lt.getName())) {
            double remaining = balRepo.getRemaining(empId, year);
            if (days > remaining) {
                throw new IllegalStateException(
                    String.format("公休假额度不足: 本次申请 %.1f 天, 剩余额度仅 %.1f 天", days, remaining));
            }
        }

        // 事假抵扣公休假
        double offsetAnnual = 0;
        if (lt != null && LeaveCalculator.isPersonalLeave(lt.getName())) {
            double remaining = balRepo.getRemaining(empId, year);
            offsetAnnual = Math.min(days, remaining);
        }

        LeaveApplication a = new LeaveApplication();
        a.setEmployeeId(empId);
        a.setLeaveTypeId(leaveTypeId);
        a.setStartDate(start);
        a.setStartPeriod(startPeriod);
        a.setEndDate(end);
        a.setEndPeriod(endPeriod);
        a.setDays(days);
        a.setReason(reason);
        a.setStatus("待审批");
        a.setOffsetAnnual(offsetAnnual);

        Long id = appRepo.insert(a);

        // 抵扣公休假余额:
        // - 事假场景: 抵扣事假天数 (offsetAnnual 已计算)
        // - 公休假场景: 抵扣公休假天数 (days)
        if (lt != null && LeaveCalculator.isAnnualLeave(lt.getName())) {
            // 公休假: 直接扣减本次天数
            balRepo.addUsed(empId, year, days);
        } else if (offsetAnnual > 0) {
            // 事假: 抵扣 offsetAnnual 天
            balRepo.addUsed(empId, year, offsetAnnual);
        }
        return id;
    }

    /**
     * 编辑请假申请: 删除旧的 (恢复抵扣) 再新增
     */
    @Transactional
    public Long updateApplication(Long appId, Long empId, Long leaveTypeId,
                                  LocalDate start, String startPeriod,
                                  LocalDate end, String endPeriod,
                                  String reason) {
        deleteApplication(appId);
        return submitApplication(empId, leaveTypeId, start, startPeriod, end, endPeriod, reason);
    }

    @Transactional
    public void approveApplication(Long appId, String approver) {
        appRepo.updateStatus(appId, "已审批", approver);
    }

    /**
     * 删除请假申请 (恢复事假抵扣的天数 / 公休假已用天数, 同时级联删除销假记录和附件)
     */
    @Transactional
    public void deleteApplication(Long appId) {
        LeaveApplication a = appRepo.findById(appId);
        if (a == null) return;
        LeaveType lt = ltRepo.findById(a.getLeaveTypeId());
        int year = a.getStartDate().getYear();
        // 恢复事假抵扣的天数
        if (a.getOffsetAnnual() != null && a.getOffsetAnnual() > 0) {
            balRepo.subtractUsed(a.getEmployeeId(), year, a.getOffsetAnnual());
        }
        // 公休假请假: 恢复本次消耗的天数
        if (lt != null && LeaveCalculator.isAnnualLeave(lt.getName())) {
            balRepo.subtractUsed(a.getEmployeeId(), year, a.getDays());
        }
        cancelRepo.deleteByApplicationId(appId);
        // 同时删除关联的附件 (SQLite 默认未启用外键级联, 显式删除)
        attachRepo.deleteByApplicationId(appId);
        appRepo.delete(appId);
    }

    // ==================== 销假管理 ====================

    public List<LeaveApplication> listPendingCancellations() {
        return appRepo.findPendingCancellations();
    }

    public List<LeaveApplication> listPendingCancellationsFiltered(
            Long deptId, Long employeeId, Long leaveTypeId,
            String startDate, String endDate) {
        return appRepo.findPendingCancellationsFiltered(deptId, employeeId, leaveTypeId, startDate, endDate);
    }

    public List<LeaveCancellation> listCancellations() {
        return cancelRepo.findAll();
    }

    @Transactional
    public Long registerCancellation(Long appId, LocalDate cancelDate, double actualDays, String remark) {
        Long id = cancelRepo.insert(appId, cancelDate, actualDays, remark);
        // 销假后状态改为 "已销假" (可选; 这里按原版逻辑只新增销假记录)
        appRepo.updateStatus(appId, "已销假", null);
        return id;
    }

    // ==================== 公休假额度 ====================

    public List<AnnualLeaveBalance> listAnnualBalances(int year, Long deptId) {
        return balRepo.findByYear(year, deptId);
    }

    /**
     * 初始化年度公休假额度:
     * 1. 删除该年度旧数据
     * 2. 按员工工龄计算额度
     * 3. 重放本年度已存在的请假记录 (事假抵扣 + 公休假请假)
     */
    @Transactional
    public int initAnnualBalance(int year) {
        balRepo.deleteByYear(year);
        List<Employee> emps = empRepo.findWorkInfoAll();
        for (Employee e : emps) {
            int wy = LeaveCalculator.calculateWorkYears(e.getWorkStartDate());
            double total = LeaveCalculator.getAnnualLeaveDays(wy);
            balRepo.insert(e.getId(), year, wy, total);
        }
        // 重放本年度已存在的请假记录
        List<LeaveApplication> apps = appRepo.findAll(year, null, null);
        for (LeaveApplication a : apps) {
            LeaveType lt = ltRepo.findById(a.getLeaveTypeId());
            if (lt == null) continue;
            // 公休假: 扣减本次天数
            if (LeaveCalculator.isAnnualLeave(lt.getName()) && a.getDays() != null && a.getDays() > 0) {
                balRepo.addUsed(a.getEmployeeId(), year, a.getDays());
            }
            // 事假抵扣
            else if (LeaveCalculator.isPersonalLeave(lt.getName()) &&
                     a.getOffsetAnnual() != null && a.getOffsetAnnual() > 0) {
                balRepo.addUsed(a.getEmployeeId(), year, a.getOffsetAnnual());
            }
        }
        return emps.size();
    }

    @Transactional
    public void updateBalance(Long balanceId, double total, double used) {
        balRepo.updateTotals(balanceId, total, used);
    }

    // ==================== 统计 / 月度 / 汇总 ====================

    public List<Map<String, Object>> getStatistics(Integer year, Long deptId) {
        return statsRepo.getStatistics(year, deptId);
    }

    public List<LeaveApplication> getMonthlySignData(int year, int month, Long deptId) {
        return statsRepo.getMonthlySignData(year, month, deptId);
    }

    public List<Map<String, Object>> getSummaryData(Integer year, Long deptId) {
        return statsRepo.getSummaryData(year, deptId);
    }
}
