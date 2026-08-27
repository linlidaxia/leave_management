// 销假管理 (含多条件查询)
(function() {
    var isAdmin = false;
    var depts = [];
    var lts = [];
    var emps = [];
    var identities = [];

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return Promise.all([
            Api.get('/api/departments'),
            Api.get('/api/leave-types'),
            Api.get('/api/employees'),
            Api.get('/api/identities')
        ]);
    }).then(function(results) {
        if (!results) return;
        depts = results[0];
        lts = results[1];
        emps = results[2];
        identities = results[3] || [];
        // 从 URL 参数读取初始筛选条件
        var params = new URLSearchParams(window.location.search);
        var initDeptId = params.get('deptId') || '';
        var initYear = params.get('year') || '';
        return load(initDeptId, initYear);
    }).catch(function(e) { console.error(e); });

    function buildDeptOpts() {
        var opts = ['<option value="">全部</option>'];
        for (var i = 0; i < depts.length; i++) opts.push('<option value="' + depts[i].id + '">' + depts[i].name + '</option>');
        return opts.join('');
    }

    function buildEmpOpts(deptId) {
        var opts = ['<option value="">全部</option>'];
        for (var i = 0; i < emps.length; i++) {
            if (deptId && emps[i].departmentId !== parseInt(deptId)) continue;
            opts.push('<option value="' + emps[i].id + '">' + emps[i].name + '</option>');
        }
        return opts.join('');
    }

    function buildLtOpts() {
        var opts = ['<option value="">全部</option>'];
        for (var i = 0; i < lts.length; i++) opts.push('<option value="' + lts[i].id + '">' + lts[i].name + '</option>');
        return opts.join('');
    }

    function buildIdentityOpts() {
        var opts = ['<option value="">全部</option>'];
        for (var i = 0; i < identities.length; i++) opts.push('<option value="' + identities[i].id + '">' + identities[i].name + '</option>');
        return opts.join('');
    }

    var pageSize = 15;
    var currentPage = 0;

    function load(initDeptId, initYear) {
        var cYear = document.getElementById('c_year') ? document.getElementById('c_year').value : '';
        var deptId = document.getElementById('c_dept') ? document.getElementById('c_dept').value : '';
        var empId = document.getElementById('c_emp') ? document.getElementById('c_emp').value : '';
        var ltId = document.getElementById('c_lt') ? document.getElementById('c_lt').value : '';
        var identityId = document.getElementById('c_identity') ? document.getElementById('c_identity').value : '';
        var sd = document.getElementById('c_sd') ? document.getElementById('c_sd').value : '';
        var ed = document.getElementById('c_ed') ? document.getElementById('c_ed').value : '';
        // 首次加载时从参数应用筛选
        if (initDeptId && !deptId) deptId = initDeptId;
        if (initYear && !cYear) cYear = initYear;

        var params = [];
        if (cYear) params.push('year=' + cYear);
        if (deptId) params.push('deptId=' + deptId);
        if (empId) params.push('employeeId=' + empId);
        if (ltId) params.push('leaveTypeId=' + ltId);
        if (identityId) params.push('identityId=' + identityId);
        if (sd) params.push('startDate=' + sd);
        if (ed) params.push('endDate=' + ed);
        params.push('page=' + currentPage);
        params.push('size=' + pageSize);
        var url = '/api/cancellations/pending?' + params.join('&');

        return Promise.all([
            Api.get(url),
            Api.get('/api/cancellations')
        ]).then(function(results) {
            var pageInfo = UI.parsePageData(results[0]);
            var pending = pageInfo.rows;
            var cancelledInfo = UI.parsePageData(results[1]);
            var cancelledAll = cancelledInfo.rows;

            // 已销假按年度筛选
            var cancelled = [];
            for (var j = 0; j < cancelledAll.length; j++) {
                if (cYear && cancelledAll[j].startDate && cancelledAll[j].startDate.indexOf(String(cYear)) !== 0) continue;
                cancelled.push(cancelledAll[j]);
            }

            var html = '<div class="page-header"><div><div class="page-title">销假管理</div><div class="page-subtitle">Leave Cancellation</div></div></div>';
            html += '<div class="page-tip">提示: 点击待销假记录的「销假」按钮进行销假登记</div>';

            // 查询条件区 (max-width 限制卡片宽度, 紧凑布局)
            html += '<div class="card" style="padding:8px 12px;">';
            html += '<div class="form-row" style="gap:6px;flex-wrap:wrap;">';
            html += '<div class="form-group" style="width:80px;flex-shrink:0;"><label>年度</label><input type="number" id="c_year" value="' + (cYear || new Date().getFullYear()) + '"></div>';
            html += '<div class="form-group" style="width:120px;flex-shrink:0;"><label>部门</label><select id="c_dept" onchange="onCancelDeptChange()">' + buildDeptOpts() + '</select></div>';
            html += '<div class="form-group" style="width:120px;flex-shrink:0;"><label>人员</label><select id="c_emp">' + buildEmpOpts(deptId) + '</select></div>';
            html += '<div class="form-group" style="width:100px;flex-shrink:0;"><label>假别</label><select id="c_lt">' + buildLtOpts() + '</select></div>';
            html += '<div class="form-group" style="width:120px;flex-shrink:0;"><label>身份</label><select id="c_identity">' + buildIdentityOpts() + '</select></div>';
            html += '<div class="form-group" style="width:130px;flex-shrink:0;"><label>开始日期</label><input type="date" id="c_sd" value="' + (sd||'') + '"></div>';
            html += '<div class="form-group" style="width:130px;flex-shrink:0;"><label>结束日期</label><input type="date" id="c_ed" value="' + (ed||'') + '"></div>';
            html += '</div>';
            html += '<div style="display:flex;gap:6px;margin-top:6px;">';
            html += '<button class="btn btn-primary" onclick="load()">查询</button>';
            html += '<button class="btn btn-secondary" onclick="cancelReset()">重置</button>';
            html += '</div>';
            html += '</div>';

            // 待销假列表
            html += '<div class="card"><h3>待销假记录 (' + pending.length + ')</h3>';
            var pRows = [];
            for (var i = 0; i < pending.length; i++) {
                var a = pending[i];
                pRows.push([a.id, a.employeeName, a.departmentName||'—', a.leaveTypeName, UI.fmtDate(a.startDate), UI.fmtDate(a.endDate), UI.fmtNum(a.days,1), UI.statusTag(a.status), a]);
            }
            html += '<div id="cancel_pending_wrap">';
            html += UI.table(['ID', '姓名', '部门', '假别', '开始', '结束', '天数', '状态', '操作'],
                pRows, [null,null,null,null,null,null,null,null, function(val, row) {
                    var a = row[8];
                    if (!isAdmin) return '<span class="text-muted">只读</span>';
                    return '<button class="btn btn-sm btn-success" onclick="cancelRegister(' + a.id + ')">销假</button>';
                }]);
            html += '</div></div>';

            // 已销假列表
            html += '<div class="card"><h3>已销假记录 (' + cancelled.length + ')</h3>';
            var cRows = [];
            for (var j = 0; j < cancelled.length; j++) {
                var c = cancelled[j];
                cRows.push([c.id, c.employeeName, c.departmentName||'—', c.leaveTypeName, UI.fmtDate(c.startDate), UI.fmtDate(c.endDate), UI.fmtNum(c.leaveDays,1), UI.fmtDate(c.cancelDate), UI.fmtNum(c.actualDays,1)]);
            }
            html += UI.table(['ID', '姓名', '部门', '假别', '开始', '结束', '原天数', '销假日期', '实际天数'], cRows);
            html += '</div>';

            document.getElementById('contentArea').innerHTML = html;
            var tw = document.querySelector('#cancel_pending_wrap .table-wrap');
            if (tw) tw.insertAdjacentHTML('afterend', UI.pagination(pageInfo.total, currentPage, pageSize, 'cancelChangePage'));
            // 恢复筛选条件
            var sdYear = document.getElementById('c_year');
            if (sdYear) sdYear.value = cYear;
            var sdDept = document.getElementById('c_dept');
            if (sdDept) sdDept.value = deptId;
            if (deptId) { var sdEmp = document.getElementById('c_emp'); if (sdEmp) sdEmp.innerHTML = buildEmpOpts(deptId); }
            var sdEmp2 = document.getElementById('c_emp');
            if (sdEmp2) sdEmp2.value = empId;
            var sdLt = document.getElementById('c_lt');
            if (sdLt) sdLt.value = ltId;
            var sdIdentity = document.getElementById('c_identity');
            if (sdIdentity) sdIdentity.value = identityId;
            var sdSd = document.getElementById('c_sd');
            if (sdSd) sdSd.value = sd;
            var sdEd = document.getElementById('c_ed');
            if (sdEd) sdEd.value = ed;
        });
    }
    window.load = load;

    window.cancelChangePage = function(p) { currentPage = p; load(); };

    window.onCancelDeptChange = function() {
        var deptId = document.getElementById('c_dept').value;
        var empSel = document.getElementById('c_emp');
        if (empSel) empSel.innerHTML = buildEmpOpts(deptId);
    };

    window.cancelReset = function() {
        document.getElementById('c_year').value = new Date().getFullYear();
        document.getElementById('c_dept').value = '';
        document.getElementById('c_emp').value = '';
        document.getElementById('c_lt').value = '';
        document.getElementById('c_identity').value = '';
        document.getElementById('c_sd').value = '';
        document.getElementById('c_ed').value = '';
        load();
    };

    window.cancelRegister = function(appId) {
        Api.get('/api/cancellations/pending').then(function(pending) {
            var a = null;
            for (var i = 0; i < pending.length; i++) if (pending[i].id === appId) a = pending[i];
            if (!a) return;
            UI.modal('销假登记',
                '<div class="form-group"><label>人员</label><input value="' + a.employeeName + '" disabled></div>' +
                '<div class="form-group"><label>假别</label><input value="' + a.leaveTypeName + '" disabled></div>' +
                '<div class="form-group"><label>请假天数</label><input value="' + UI.fmtNum(a.days,1) + '" disabled></div>' +
                '<div class="form-group"><label>实际天数 *</label><input type="number" step="0.5" id="c_actual" value="' + a.days + '"></div>' +
                '<div class="form-group"><label>销假日期 *</label><input type="date" id="c_date" value="' + UI.today() + '"></div>' +
                '<div class="form-group"><label>备注</label><input id="c_remark"></div>',
                function(form) {
                    var actual = parseFloat(form.querySelector('#c_actual').value);
                    var dt = form.querySelector('#c_date').value;
                    if (isNaN(actual) || !dt) { UI.toast('请填写实际天数和销假日期', 'error'); throw new Error(); }
                    return Api.post('/api/cancellations', {
                        applicationId: appId, cancelDate: dt, actualDays: actual,
                        remark: form.querySelector('#c_remark').value
                    }).then(function(r) {
                        if (r.success) { UI.closeModal(); UI.toast('销假登记成功', 'success'); load(); }
                        else { UI.toast(r.message, 'error'); throw new Error(); }
                    });
                },
                '确认销假'
            );
        });
    };
})();
