// 首页概览
(function() {
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        return Api.get('/api/dashboard');
    }).then(function(s) {
        if (!s) return;
        var c = document.getElementById('contentArea');
        c.innerHTML =
            '<div class="page-header">' +
                '<div><div class="page-title">首页概览</div><div class="page-subtitle">Dashboard · 系统总览</div></div>' +
            '</div>' +
            '<div class="stats-grid">' +
                '<div class="stat-card"><div class="stat-num" style="color:var(--ink)">' + s.deptCount + '</div><div class="stat-label">部门总数</div></div>' +
                '<div class="stat-card"><div class="stat-num" style="color:var(--success)">' + s.empCount + '</div><div class="stat-label">人员总数</div></div>' +
                '<div class="stat-card"><div class="stat-num" style="color:var(--warning)">' + s.appCount + '</div><div class="stat-label">本年请假</div></div>' +
                '<div class="stat-card"><div class="stat-num" style="color:var(--danger)">' + s.pendingCount + '</div><div class="stat-label">待审批</div></div>' +
                '<div class="stat-card"><div class="stat-num" style="color:var(--info)">' + s.cancelPending + '</div><div class="stat-label">待销假</div></div>' +
            '</div>' +
            '<div class="card"><h3>使用指引</h3>' +
            '<div style="line-height:2;color:var(--text-2);font-size:13px;">' +
                '<strong style="color:var(--gold-dark);">1.</strong> 在「部门管理」中添加部门<br>' +
                '<strong style="color:var(--gold-dark);">2.</strong> 在「人员管理」中录入员工信息（填写参加工作时间，系统自动计算工龄）<br>' +
                '<strong style="color:var(--gold-dark);">3.</strong> 在「公休假额度」中初始化年度公休假<br>' +
                '<strong style="color:var(--gold-dark);">4.</strong> 在「请假登记」中录入请假记录<br>' +
                '<strong style="color:var(--gold-dark);">5.</strong> 事假可自动抵扣公休假，半天按 0.5 天计算' +
            '</div></div>';
    }).catch(function(e) { console.error(e); });
})();
