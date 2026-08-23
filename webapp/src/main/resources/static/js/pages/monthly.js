// 月度签字表
(function() {
    var deptOpts = '';
    var now = new Date();
    var initYear = now.getFullYear(), initMonth = now.getMonth() + 1;
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        return Api.get('/api/departments');
    }).then(function(list) {
        if (!list) return;
        var opts = ['<option value="">全部</option>'];
        for (var i = 0; i < list.length; i++) opts.push('<option value="' + list[i].id + '">' + list[i].name + '</option>');
        deptOpts = opts.join('');
        return load(initYear, initMonth);
    }).catch(function(e) { console.error(e); });

    function load(year, month) {
        return Api.get('/api/stats/monthly?year=' + year + '&month=' + month).then(function(rows) {
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var a = rows[i];
                tRows.push([a.employeeName, a.departmentName||'—', a.leaveTypeName, UI.fmtDate(a.startDate), UI.fmtDate(a.endDate), UI.fmtNum(a.days,1), a.reason||'—', UI.statusTag(a.status), '']);
            }
            var monthOpts = [];
            for (var i = 1; i <= 12; i++) monthOpts.push('<option value="' + i + '"' + (i === month ? ' selected' : '') + '>' + i + ' 月</option>');
            var html =
                '<div class="page-header"><div><div class="page-title">月度签字表</div><div class="page-subtitle">Monthly Sign-off Sheet</div></div></div>' +
                '<div class="toolbar">' +
                    '<label>年度</label><input type="number" id="m_year" value="' + year + '" style="width:80px;">' +
                    '<label>月份</label><select id="m_month">' + monthOpts.join('') + '</select>' +
                    '<label>部门</label><select id="m_dept">' + deptOpts + '</select>' +
                    '<button class="btn btn-primary" onclick="monthlyQuery()">查询</button>' +
                    '<button class="btn btn-secondary" onclick="monthlyExport()">导出 Excel</button>' +
                '</div>' +
                UI.table(['姓名', '部门', '假别', '开始', '结束', '天数', '事由', '状态', '签字'], tRows);
            document.getElementById('contentArea').innerHTML = html;
        });
    }
    window.load = load;

    window.monthlyQuery = function() {
        var y = document.getElementById('m_year').value;
        var m = document.getElementById('m_month').value;
        var d = document.getElementById('m_dept').value;
        Api.get('/api/stats/monthly?year=' + y + '&month=' + m + (d ? '&deptId=' + d : '')).then(function(rows) {
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var a = rows[i];
                tRows.push([a.employeeName, a.departmentName||'—', a.leaveTypeName, UI.fmtDate(a.startDate), UI.fmtDate(a.endDate), UI.fmtNum(a.days,1), a.reason||'—', UI.statusTag(a.status), '']);
            }
            document.querySelector('#contentArea .table-wrap').outerHTML = UI.table(['姓名', '部门', '假别', '开始', '结束', '天数', '事由', '状态', '签字'], tRows);
        });
    };

    window.monthlyExport = function() {
        var y = document.getElementById('m_year').value;
        var m = document.getElementById('m_month').value;
        var d = document.getElementById('m_dept').value;
        UI.downloadExcel('/api/export/monthly?year=' + y + '&month=' + m + (d ? '&deptId=' + d : ''));
    };
})();
