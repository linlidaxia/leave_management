package com.leavemgmt.model;

public class LeaveType {
    private Long id;
    private String name;
    private String code;
    private Integer sortOrder;
    private Boolean needAttachment;   // 是否需要佐证附件
    private Boolean deductFromAnnual; // 是否优先扣除公休假
    private String remark;

    public LeaveType() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getNeedAttachment() { return needAttachment; }
    public void setNeedAttachment(Boolean needAttachment) { this.needAttachment = needAttachment; }
    public Boolean getDeductFromAnnual() { return deductFromAnnual; }
    public void setDeductFromAnnual(Boolean deductFromAnnual) { this.deductFromAnnual = deductFromAnnual; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
