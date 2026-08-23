package com.leavemgmt.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页统计卡片数据
 */
public class DashboardStats {
    private long deptCount;
    private long empCount;
    private long appCount;
    private long pendingCount;       // 待审批数
    private long cancelPending;      // 待销假数

    public long getDeptCount() { return deptCount; }
    public void setDeptCount(long deptCount) { this.deptCount = deptCount; }
    public long getEmpCount() { return empCount; }
    public void setEmpCount(long empCount) { this.empCount = empCount; }
    public long getAppCount() { return appCount; }
    public void setAppCount(long appCount) { this.appCount = appCount; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public long getCancelPending() { return cancelPending; }
    public void setCancelPending(long cancelPending) { this.cancelPending = cancelPending; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("deptCount", deptCount);
        m.put("empCount", empCount);
        m.put("appCount", appCount);
        m.put("pendingCount", pendingCount);
        m.put("cancelPending", cancelPending);
        return m;
    }
}
