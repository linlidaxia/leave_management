package com.leavemgmt.model;

import java.time.LocalDate;

/**
 * 请假申请记录 (含 join 字段)
 *
 * status 取值: 待审批 / 已审批
 */
public class LeaveApplication {
    private Long id;
    private Long employeeId;
    private String employeeName;        // join
    private String departmentName;     // join
    private Long leaveTypeId;
    private String leaveTypeName;       // join
    private LocalDate startDate;
    private String startPeriod;
    private LocalDate endDate;
    private String endPeriod;
    private Double days;
    private String reason;
    private String status;
    private LocalDate applyDate;
    private String approver;
    private LocalDate approveDate;
    private Double offsetAnnual;
    private String remark;

    public LeaveApplication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public String getStartPeriod() { return startPeriod; }
    public void setStartPeriod(String startPeriod) { this.startPeriod = startPeriod; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getEndPeriod() { return endPeriod; }
    public void setEndPeriod(String endPeriod) { this.endPeriod = endPeriod; }
    public Double getDays() { return days; }
    public void setDays(Double days) { this.days = days; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getApplyDate() { return applyDate; }
    public void setApplyDate(LocalDate applyDate) { this.applyDate = applyDate; }
    public String getApprover() { return approver; }
    public void setApprover(String approver) { this.approver = approver; }
    public LocalDate getApproveDate() { return approveDate; }
    public void setApproveDate(LocalDate approveDate) { this.approveDate = approveDate; }
    public Double getOffsetAnnual() { return offsetAnnual; }
    public void setOffsetAnnual(Double offsetAnnual) { this.offsetAnnual = offsetAnnual; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
