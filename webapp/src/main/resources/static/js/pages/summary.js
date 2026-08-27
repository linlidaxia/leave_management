// 汇总表 (假别横排, 双行表头)
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

    function pivotData(rows) {
        var ltSet = {};
        var ltOrder = [];
        var deptMap = {};
        var deptOrder = [];
        for (var i = 0; i < rows.length; i++) {
            var r = rows[i];
            var lt = r.leave_type || '（未知）';
            if (!ltSet[lt]) { ltSet[lt] = true; ltOrder.push(lt); }
            var dept = r.dept_name || '（未分配）';
            if (!deptMap[dept]) {
                deptMap[dept] = { days: {}, cnts: {} };
                deptOrder.push(dept);
            }
            deptMap[dept].days[lt] = (deptMap[dept].days[lt] || 0) + r.total_days;
            deptMap[dept].cnts[lt] = (deptMap[dept].cnts[lt] || 0) + r.cnt;
        }
        return { ltOrder: ltOrder, deptMap: deptMap, deptOrder: deptOrder };
    }

    function buildTable(pivoted) {
        var html = '<div class="sum-table-wrap"><table class="data-table sum-table" style="border-collapse:separate;border-spacing:0;">';

        html += '<style>';
        html += '.lt-group{border-left:2px solid #b0b8c4;}';
        html += '.lt-group-first{border-left:none;}';
        html += '.lt-header{border-bottom:2px solid #8892a0;text-align:center;font-weight:600;background:#EFEBE0;}';
        html += '.lt-sub{border-bottom:2px solid #8892a0;text-align:center;font-size:12px;color:var(--text-2);font-weight:500;background:#EFEBE0;}';
        html += '.lt-cell{border-left:2px solid #b0b8c4;border-bottom:1px solid #d8dde4;text-align:center;padding:4px 8px;}';
        html += '.lt-cell-first{border-left:1px solid #d8dde4;border-bottom:1px solid #d8dde4;text-align:center;padding:4px 8px;}';
        html += '.lt-noleft{border-left:none !important;}';
        html += '.lt-dash{border-right:1px dashed #aaa !important;}';
        html += '.lt-total{border-left:2px solid #8892a0 !important;font-weight:600;text-align:center;border-bottom:1px solid #d8dde4;padding:4px 8px;}';
        html += '.dept-cell{border-bottom:1px solid #d8dde4;border-right:2px solid #b0b8c4;font-weight:500;padding:4px 8px;}';
        html += '.sum-thead th{position:sticky;top:0;z-index:2;background:#EFEBE0;}';
        html += '.sum-table-wrap{max-height:calc(100vh - 200px);overflow-y:auto;}';
        html += '</style>';

        html += '<thead class="sum-thead">';
        html += '<tr>';
        html += '<th rowspan="2" class="dept-cell" style="border-bottom:2px solid #8892a0;">部门</th>';
        for (var k = 0; k < pivoted.ltOrder.length; k++) {
            var cls = k === 0 ? 'lt-group lt-group-first lt-header' : 'lt-group lt-header';
            html += '<th colspan="2" class="' + cls + '">' + pivoted.ltOrder[k] + '</th>';
        }
        html += '<th rowspan="2" class="lt-total" style="border-bottom:2px solid #8892a0;">合计天数</th>';
        html += '</tr>';
        html += '<tr>';
        for (var k = 0; k < pivoted.ltOrder.length; k++) {
            var cls = k === 0 ? 'lt-group lt-group-first lt-sub' : 'lt-group lt-sub';
            html += '<th class="' + cls + '">人次</th>';
            html += '<th class="' + cls + '">天数</th>';
        }
        html += '</tr>';
        html += '</thead><tbody>';

        for (var i = 0; i < pivoted.deptOrder.length; i++) {
            var dept = pivoted.deptOrder[i];
            var d = pivoted.deptMap[dept];
            html += '<tr>';
            html += '<td class="dept-cell">' + dept + '</td>';
            var totalDays = 0;
            for (var j = 0; j < pivoted.ltOrder.length; j++) {
                var lt = pivoted.ltOrder[j];
                var days = d.days[lt] || 0;
                var cnt = d.cnts[lt] || 0;
                var cls = j === 0 ? 'lt-cell lt-cell-first' : 'lt-cell';
                html += '<td class="' + cls + ' lt-dash">' + (cnt > 0 ? cnt : '—') + '</td>';
                html += '<td class="' + cls + ' lt-noleft">' + (days > 0 ? UI.fmtNum(days, 1) : '—') + '</td>';
                totalDays += days;
            }
            html += '<td class="lt-total">' + UI.fmtNum(totalDays, 1) + '</td>';
            html += '</tr>';
        }

        html += '</tbody></table></div>';
        return html;
    }

    function load(year) {
        return Api.get('/api/stats/summary?year=' + year).then(function(rows) {
            var pivoted = pivotData(rows);
            var html =
                '<div class="page-header"><div><div class="page-title">汇总表</div><div class="page-subtitle">Summary Report</div></div></div>' +
                '<div class="toolbar">' +
                    '<label>年度</label><input type="number" id="sum_year" value="' + year + '" style="width:80px;">' +
                    '<label>部门</label><select id="sum_dept">' + deptOpts + '</select>' +
                    '<button class="btn btn-primary" onclick="summaryQuery()">查询</button>' +
                    '<button class="btn btn-secondary" onclick="summaryExport()">导出 Excel</button>' +
                '</div>' +
                '<div class="card" style="padding:0;overflow:hidden;">' + buildTable(pivoted) + '</div>';
            document.getElementById('contentArea').innerHTML = html;
        });
    }
    window.load = load;

    window.summaryQuery = function() {
        var y = document.getElementById('sum_year').value;
        var d = document.getElementById('sum_dept').value;
        Api.get('/api/stats/summary?year=' + y + (d ? '&deptId=' + d : '')).then(function(rows) {
            var pivoted = pivotData(rows);
            var oldTable = document.querySelector('#contentArea .sum-table-wrap');
            if (oldTable) oldTable.outerHTML = '<div class="sum-table-wrap">' + buildTableInner(pivoted) + '</div>';
        });
    };

    window.summaryExport = function() {
        var y = document.getElementById('sum_year').value;
        var d = document.getElementById('sum_dept').value;
        UI.downloadExcel('/api/export/summary?year=' + y + (d ? '&deptId=' + d : ''));
    };
})();
