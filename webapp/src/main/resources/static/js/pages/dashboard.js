// 首页概览
(function() {
    var tm = window.parent && window.parent !== window ? window.parent.TabManager : TabManager;
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        return Api.get('/api/dashboard');
    }).then(function(s) {
        if (!s) return;
        var year = new Date().getFullYear();
        var c = document.getElementById('contentArea');
        c.innerHTML =
            '<div class="page-header">' +
                '<div><div class="page-title">首页概览</div><div class="page-subtitle">Dashboard · 系统总览</div></div>' +
            '</div>' +
            '<div class="stats-grid">' +
                '<a class="stat-card stat-link" href="javascript:void(0)" onclick="window.parent.TabManager.open(\'departments\',\'部门管理\',\'/departments.html\')">' +
                    '<div class="stat-num" style="color:var(--ink)">' + s.deptCount + '</div>' +
                    '<div class="stat-label">部门总数</div>' +
                '</a>' +
                '<a class="stat-card stat-link" href="javascript:void(0)" onclick="window.parent.TabManager.open(\'employees\',\'人员管理\',\'/employees.html\')">' +
                    '<div class="stat-num" style="color:var(--success)">' + s.empCount + '</div>' +
                    '<div class="stat-label">人员总数</div>' +
                '</a>' +
                '<a class="stat-card stat-link" href="javascript:void(0)" onclick="window.parent.TabManager.open(\'leave-list\',\'请假记录\',\'/leave-list.html?year=' + year + '\')">' +
                    '<div class="stat-num" style="color:var(--warning)">' + s.appCount + '</div>' +
                    '<div class="stat-label">本年请假</div>' +
                '</a>' +
                '<a class="stat-card stat-link" href="javascript:void(0)" onclick="window.parent.TabManager.open(\'leave-list\',\'请假记录\',\'/leave-list.html?year=' + year + '&status=%E5%BE%85%E5%AE%A1%E6%89%B9\')">' +
                    '<div class="stat-num" style="color:var(--danger)">' + s.pendingCount + '</div>' +
                    '<div class="stat-label">待审批</div>' +
                '</a>' +
                '<a class="stat-card stat-link" href="javascript:void(0)" onclick="window.parent.TabManager.open(\'cancel\',\'销假管理\',\'/cancel.html?year=' + year + '\')">' +
                    '<div class="stat-num" style="color:var(--info)">' + s.cancelPending + '</div>' +
                    '<div class="stat-label">待销假</div>' +
                '</a>' +
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
