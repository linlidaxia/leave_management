package com.leavemgmt.model;

import java.time.LocalDate;

/**
 * 销假记录
 */
public class LeaveCancellation {
    private Long id;
    private Long applicationId;
    private LocalDate cancelDate;
    private Double actualDays;
    private String remark;

    // 以下为 join 字段 (列表展示用)
    private String employeeName;
    private String departmentName;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double leaveDays;

    public LeaveApplication application; // 关联的完整申请 (用于销假登记时展示)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public LocalDate getCancelDate() { return cancelDate; }
    public void setCancelDate(LocalDate cancelDate) { this.cancelDate = cancelDate; }
    public Double getActualDays() { return actualDays; }
    public void setActualDays(Double actualDays) { this.actualDays = actualDays; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Double getLeaveDays() { return leaveDays; }
    public void setLeaveDays(Double leaveDays) { this.leaveDays = leaveDays; }
}
