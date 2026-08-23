// 公休假额度
(function() {
    var isAdmin = false;
    var pageSize = 15;
    var currentPage = 0;

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return load(UI.currentYear());
    }).catch(function(e) { console.error(e); });

    function load(year) {
        return Api.get('/api/annual-balances?year=' + year + '&page=' + currentPage + '&size=' + pageSize).then(function(resp) {
            var pageInfo = UI.parsePageData(resp);
            var rows = pageInfo.rows;
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var b = rows[i];
                tRows.push([b.id, b.employeeName, b.departmentName||'—', b.workYears, UI.fmtNum(b.totalDays,1), UI.fmtNum(b.usedDays,1), UI.fmtNum(b.remainingDays,1), b]);
            }
            var html = '<div class="page-header"><div><div class="page-title">公休假额度</div><div class="page-subtitle">Annual Leave Balance</div></div></div>';
            html += '<div class="page-tip">公休假规则: 工龄 &lt; 10 年 = 5 天 · 10~20 年 = 10 天 · ≥ 20 年 = 15 天</div>';
            html += '<div class="toolbar">' +
                '<label>年度</label><input type="number" id="b_year" value="' + year + '" style="width:80px;">' +
                '<button class="btn btn-primary" onclick="balanceQuery()">查询</button>' +
                (isAdmin ? '<button class="btn btn-gold" onclick="balanceInit()">初始化额度</button>' : '') +
                '<button class="btn btn-secondary" onclick="balanceExport()">导出 Excel</button></div>';
            html += UI.table(['ID', '姓名', '部门', '工龄', '总额度(天)', '已用(天)', '剩余(天)', '操作'],
                tRows, [null,null,null,null,null,null,null, function(val, row) {
                    var b = row[7];
                    if (!isAdmin) return '<span class="text-muted">只读</span>';
                    return '<button class="btn btn-sm btn-primary" onclick="balanceEdit(' + b.id + ')">编辑</button>';
                }]);
            document.getElementById('contentArea').innerHTML = html;
            var tw = document.querySelector('#contentArea .table-wrap');
            if (tw) tw.insertAdjacentHTML('afterend', UI.pagination(pageInfo.total, currentPage, pageSize, 'balanceChangePage'));
        });
    }
    window.load = load;

    window.balanceChangePage = function(p) { currentPage = p; load(document.getElementById('b_year').value); };

    window.balanceQuery = function() {
        var y = parseInt(document.getElementById('b_year').value);
        load(y);
    };

    window.balanceInit = function() {
        var y = document.getElementById('b_year').value;
        if (!confirm('确定要初始化 ' + y + ' 年度公休假额度吗?\n这将重新计算所有员工的公休假额度。')) return;
        Api.post('/api/annual-balances/init?year=' + y, {}).then(function(r) {
            if (r.success) { UI.toast(r.message, 'error'); balanceQuery(); }
            else UI.toast(r.message, 'error');
        });
    };

    window.balanceEdit = function(id) {
        var year = parseInt(document.getElementById('b_year').value);
        Api.get('/api/annual-balances?year=' + year).then(function(rows) {
            var b = null;
            for (var i = 0; i < rows.length; i++) if (rows[i].id === id) b = rows[i];
            if (!b) return;
            UI.modal('编辑公休假额度',
                '<div class="form-group"><label>姓名</label><input value="' + b.employeeName + '" disabled></div>' +
                '<div class="form-group"><label>工龄</label><input value="' + b.workYears + ' 年" disabled></div>' +
                '<div class="form-group"><label>总额度(天)</label><input type="number" step="0.5" id="be_total" value="' + b.totalDays + '"></div>' +
                '<div class="form-group"><label>已用(天)</label><input type="number" step="0.5" id="be_used" value="' + b.usedDays + '"></div>' +
                '<div class="hint">剩余: <span id="be_remaining">' + UI.fmtNum(b.remainingDays,1) + '</span> 天</div>',
                function(form) {
                    var total = parseFloat(form.querySelector('#be_total').value);
                    var used = parseFloat(form.querySelector('#be_used').value);
                    if (isNaN(total) || isNaN(used)) { UI.toast('请输入有效数字', 'error'); throw new Error(); }
                    return Api.put('/api/annual-balances/' + id, { totalDays: total, usedDays: used }).then(function(r) {
                        if (r.success) { UI.closeModal(); balanceQuery(); }
                        else { UI.toast(r.message, 'error'); throw new Error(); }
                    });
                },
                '保存'
            );
            var updRem = function() {
                var t = parseFloat(document.getElementById('be_total').value) || 0;
                var u = parseFloat(document.getElementById('be_used').value) || 0;
                document.getElementById('be_remaining').textContent = (t - u).toFixed(1);
            };
            document.getElementById('be_total').oninput = updRem;
            document.getElementById('be_used').oninput = updRem;
        });
    };

    window.balanceExport = function() {
        var y = document.getElementById('b_year').value;
        UI.downloadExcel('/api/export/annual-balance?year=' + y);
    };
})();
