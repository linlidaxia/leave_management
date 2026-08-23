package com.leavemgmt.model;

public class AnnualLeaveBalance {
    private Long id;
    private Long employeeId;
    private String employeeName;        // join
    private String departmentName;     // join
    private Integer yearVal;
    private Integer workYears;
    private Double totalDays;
    private Double usedDays;
    private Double remainingDays;
    private String remark;

    public AnnualLeaveBalance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Integer getYearVal() { return yearVal; }
    public void setYearVal(Integer yearVal) { this.yearVal = yearVal; }
    public Integer getWorkYears() { return workYears; }
    public void setWorkYears(Integer workYears) { this.workYears = workYears; }
    public Double getTotalDays() { return totalDays; }
    public void setTotalDays(Double totalDays) { this.totalDays = totalDays; }
    public Double getUsedDays() { return usedDays; }
    public void setUsedDays(Double usedDays) { this.usedDays = usedDays; }
    public Double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Double remainingDays) { this.remainingDays = remainingDays; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
