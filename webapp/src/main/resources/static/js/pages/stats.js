// 统计表
(function() {
    var deptOpts = '';
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        return Api.get('/api/departments');
    }).then(function(list) {
        if (!list) return;
        var opts = ['<option value="">全部</option>'];
        for (var i = 0; i < list.length; i++) opts.push('<option value="' + list[i].id + '">' + list[i].name + '</option>');
        deptOpts = opts.join('');
        return load(UI.currentYear());
    }).catch(function(e) { console.error(e); });

    function load(year) {
        return Api.get('/api/stats/statistics?year=' + year).then(function(rows) {
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var r = rows[i];
                tRows.push([r.emp_name, r.dept_name || '（未分配）', r.leave_type, UI.fmtNum(r.total_days,1), r.cnt]);
            }
            var html =
                '<div class="page-header"><div><div class="page-title">统计表</div><div class="page-subtitle">Statistics Report</div></div></div>' +
                '<div class="toolbar">' +
                    '<label>年度</label><input type="number" id="s_year" value="' + year + '" style="width:80px;">' +
                    '<label>部门</label><select id="s_dept">' + deptOpts + '</select>' +
                    '<button class="btn btn-primary" onclick="statsQuery()">查询</button>' +
                    '<button class="btn btn-secondary" onclick="statsExport()">导出 Excel</button>' +
                '</div>' +
                UI.table(['姓名', '部门', '假别', '总天数', '次数'], tRows);
            document.getElementById('contentArea').innerHTML = html;
        });
    }
    window.load = load;

    window.statsQuery = function() {
        var y = document.getElementById('s_year').value;
        var d = document.getElementById('s_dept').value;
        Api.get('/api/stats/statistics?year=' + y + (d ? '&deptId=' + d : '')).then(function(rows) {
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var r = rows[i];
                tRows.push([r.emp_name, r.dept_name || '（未分配）', r.leave_type, UI.fmtNum(r.total_days,1), r.cnt]);
            }
            document.querySelector('#contentArea .table-wrap').outerHTML = UI.table(['姓名', '部门', '假别', '总天数', '次数'], tRows);
        });
    };

    window.statsExport = function() {
        var y = document.getElementById('s_year').value;
        var d = document.getElementById('s_dept').value;
        UI.downloadExcel('/api/export/statistics?year=' + y + (d ? '&deptId=' + d : ''));
    };
})();
