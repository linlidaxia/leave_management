// 汇总表
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
        return Api.get('/api/stats/summary?year=' + year).then(function(rows) {
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var r = rows[i];
                tRows.push([r.dept_name || '（未分配）', r.leave_type, UI.fmtNum(r.total_days,1), r.cnt]);
            }
            var html =
                '<div class="page-header"><div><div class="page-title">汇总表</div><div class="page-subtitle">Summary Report</div></div></div>' +
                '<div class="toolbar">' +
                    '<label>年度</label><input type="number" id="sum_year" value="' + year + '" style="width:80px;">' +
                    '<label>部门</label><select id="sum_dept">' + deptOpts + '</select>' +
                    '<button class="btn btn-primary" onclick="summaryQuery()">查询</button>' +
                    '<button class="btn btn-secondary" onclick="summaryExport()">导出 Excel</button>' +
                '</div>' +
                UI.table(['部门', '假别', '总天数', '人次'], tRows);
            document.getElementById('contentArea').innerHTML = html;
        });
    }
    window.load = load;

    window.summaryQuery = function() {
        var y = document.getElementById('sum_year').value;
        var d = document.getElementById('sum_dept').value;
        Api.get('/api/stats/summary?year=' + y + (d ? '&deptId=' + d : '')).then(function(rows) {
            var tRows = [];
            for (var i = 0; i < rows.length; i++) {
                var r = rows[i];
                tRows.push([r.dept_name || '（未分配）', r.leave_type, UI.fmtNum(r.total_days,1), r.cnt]);
            }
            document.querySelector('#contentArea .table-wrap').outerHTML = UI.table(['部门', '假别', '总天数', '人次'], tRows);
        });
    };

    window.summaryExport = function() {
        var y = document.getElementById('sum_year').value;
        var d = document.getElementById('sum_dept').value;
        UI.downloadExcel('/api/export/summary?year=' + y + (d ? '&deptId=' + d : ''));
    };
})();
