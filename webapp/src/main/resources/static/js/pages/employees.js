// 人员管理
(function() {
    var isAdmin = false;
    var depts = [];
    var identities = [];
    var deptOpts = '';
    var identityOpts = '';

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return Promise.all([
            Api.get('/api/departments'),
            Api.get('/api/identities')
        ]).then(function(results) {
            depts = results[0];
            identities = results[1];
            var opts = ['<option value="">全部</option>'];
            for (var i = 0; i < depts.length; i++) opts.push('<option value="' + depts[i].id + '">' + depts[i].name + '</option>');
            deptOpts = opts.join('');
            var iopts = ['<option value="">全部</option>'];
            for (var j = 0; j < identities.length; j++) iopts.push('<option value="' + identities[j].id + '">' + identities[j].name + '</option>');
            identityOpts = iopts.join('');
            return load();
        });
    }).catch(function(e) { console.error(e); });

    var pageSize = 15;
    var currentPage = 0;

    function load() {
        var deptId = document.getElementById('e_dept') ? document.getElementById('e_dept').value : '';
        var identityId = document.getElementById('e_identity') ? document.getElementById('e_identity').value : '';
        var kw = document.getElementById('e_kw') ? document.getElementById('e_kw').value.trim() : '';
        var params = [];
        if (deptId) params.push('deptId=' + deptId);
        if (identityId) params.push('identityId=' + identityId);
        params.push('keyword=' + encodeURIComponent(kw));
        params.push('page=' + currentPage);
        params.push('size=' + pageSize);
        var url = '/api/employees?' + params.join('&');
        return Api.get(url).then(function(resp) {
            var pageInfo = UI.parsePageData(resp);
            var emps = pageInfo.rows;
            var rows = [];
            var year = UI.currentYear();
            for (var i = 0; i < emps.length; i++) {
                var e = emps[i];
                var remainingStyle = e.annualRemaining < 3 ? 'color:var(--danger);font-weight:600;' : '';
                rows.push([
                    e.id, e.name, e.gender||'—', e.idCard||'—', e.departmentName||'—', e.identityName||'—', e.position||'—',
                    UI.fmtDate(e.workStartDate), e.workYears,
                    UI.fmtNum(e.annualTotalActual, 1),
                    (e.annualUsed > 0 ? '<a href="#" title="查看该人员年假相关请假记录" onclick="openAnnualRelated(' + e.id + ',' + e.departmentId + ');return false;" style="color:var(--warning);font-weight:600;text-decoration:underline;">' + UI.fmtNum(e.annualUsed, 1) + '</a>' : '<span>' + UI.fmtNum(e.annualUsed, 1) + '</span>'),
                    '<span style="' + remainingStyle + '">' + UI.fmtNum(e.annualRemaining, 1) + '</span>',
                    e.phone||'—', e
                ]);
            }
            var html = '<div class="page-header"><div><div class="page-title">人员管理</div><div class="page-subtitle">Employee Management</div></div></div>';
            html += '<div class="page-tip">提示: 系统根据参加工作时间自动计算工龄；公休假额度按规则计算, 需在「公休假额度」中初始化后精确统计</div>';
            html += '<div class="toolbar">' +
                '<label>部门</label><select id="e_dept">' + deptOpts + '</select>' +
                '<label>身份</label><select id="e_identity">' + identityOpts + '</select>' +
                '<label>搜索</label><input id="e_kw" placeholder="姓名/身份证号">' +
                '<button class="btn btn-primary" onclick="load()">查询</button>';
            if (isAdmin) {
                html += '<button class="btn btn-gold" onclick="empEdit()">+ 添加人员</button>';
                html += '<button class="btn btn-success" onclick="empImport()">↓ 从 Excel 导入</button>';
            }
            html += '</div>';
            html += UI.table(
                ['ID', '姓名', '性别', '身份证号', '部门', '身份', '职务', '参加工作时间', '工龄', '当年额度(天)', '已请(天)', '剩余(天)', '电话', '操作'],
                rows,
                [null,null,null,null,null,null,null,null,null,null,null,null,null, function(val, row) {
                    var e = row[13];
                    if (!isAdmin) return '<span class="text-muted">只读</span>';
                    return '<button class="btn btn-sm btn-primary" onclick="empEdit(' + e.id + ')">编辑</button> ' +
                           '<button class="btn btn-sm btn-danger" onclick="empDelete(' + e.id + ',\'' + (e.name||'').replace(/'/g,"\\'") + '\')">删除</button>';
                }]
            );
            document.getElementById('contentArea').innerHTML = html;
            var tw = document.querySelector('#contentArea .table-wrap');
            if (tw) tw.insertAdjacentHTML('afterend', UI.pagination(pageInfo.total, currentPage, pageSize, 'empChangePage'));
            // 恢复筛选条件
            var sd = document.getElementById('e_dept');
            if (sd) sd.value = deptId;
            var si = document.getElementById('e_identity');
            if (si) si.value = identityId;
            var sk = document.getElementById('e_kw');
            if (sk) sk.value = kw;
        });
    }
    window.load = load;

    window.empChangePage = function(p) { currentPage = p; load(); };

    window.openAnnualRelated = function(empId, deptId) {
        window.parent.TabManager.open('leave-list', '请假记录', '/leave-list.html?deptId=' + deptId + '&empId=' + empId + '&annualRelated=1');
    };

    window.empEdit = function(id) {
        var initData = { name: '', gender: '男', idCard: '', departmentId: null, identityId: null, position: '', workStartDate: '', phone: '', remark: '' };
        var p = id ? Api.get('/api/employees').then(function(list) {
            for (var i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
            return initData;
        }) : Promise.resolve(initData);

        p.then(function(e) {
            var opts = ['<option value="">-- 请选择 --</option>'];
            for (var i = 0; i < depts.length; i++) {
                opts.push('<option value="' + depts[i].id + '"' + (e.departmentId === depts[i].id ? ' selected' : '') + '>' + depts[i].name + '</option>');
            }
            var iopts = ['<option value="">-- 请选择 --</option>'];
            for (var j = 0; j < identities.length; j++) {
                iopts.push('<option value="' + identities[j].id + '"' + (e.identityId === identities[j].id ? ' selected' : '') + '>' + identities[j].name + '</option>');
            }
            UI.modal(id ? '编辑人员' : '添加人员',
                '<div class="form-row">' +
                    '<div class="form-group"><label>姓名 *</label><input id="e_name" value="' + (e.name||'').replace(/"/g,'&quot;') + '"></div>' +
                    '<div class="form-group"><label>性别</label><select id="e_gender"><option ' + (e.gender==='男'?'selected':'') + '>男</option><option ' + (e.gender==='女'?'selected':'') + '>女</option></select></div>' +
                '</div>' +
                '<div class="form-group"><label>身份证号</label><input id="e_idcard" value="' + (e.idCard||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="form-row">' +
                    '<div class="form-group"><label>部门</label><select id="e_dept_sel">' + opts.join('') + '</select></div>' +
                    '<div class="form-group"><label>人员身份</label><select id="e_identity_sel">' + iopts.join('') + '</select></div>' +
                '</div>' +
                '<div class="form-row">' +
                    '<div class="form-group"><label>职务</label><input id="e_position" value="' + (e.position||'').replace(/"/g,'&quot;') + '"></div>' +
                    '<div class="form-group"><label>参加工作时间 *</label><input type="date" id="e_workstart" value="' + UI.fmtDate(e.workStartDate) + '"></div>' +
                '</div>' +
                '<div class="form-row">' +
                    '<div class="form-group"><label>电话</label><input id="e_phone" value="' + (e.phone||'').replace(/"/g,'&quot;') + '"></div>' +
                '</div>' +
                '<div class="form-group"><label>备注</label><input id="e_remark" value="' + (e.remark||'').replace(/"/g,'&quot;') + '"></div>',
                function(form) {
                    var name = form.querySelector('#e_name').value.trim();
                    if (!name) { UI.toast('请输入姓名', 'error'); throw new Error(); }
                    var deptVal = form.querySelector('#e_dept_sel').value;
                    var identityVal = form.querySelector('#e_identity_sel').value;
                    return Api.post('/api/employees', {
                        id: id || null, name: name,
                        gender: form.querySelector('#e_gender').value,
                        idCard: form.querySelector('#e_idcard').value.trim(),
                        departmentId: deptVal ? parseInt(deptVal) : null,
                        identityId: identityVal ? parseInt(identityVal) : null,
                        position: form.querySelector('#e_position').value.trim(),
                        workStartDate: form.querySelector('#e_workstart').value || null,
                        phone: form.querySelector('#e_phone').value.trim(),
                        remark: form.querySelector('#e_remark').value.trim()
                    }).then(function(r) {
                        if (!r.success) { UI.toast(r.message, 'error'); throw new Error(); }
                        UI.closeModal(); load();
                    });
                },
                '保存'
            );
        });
    };

    window.empDelete = function(id, name) {
        UI.confirm('确定要删除人员「' + name + '」吗?', function() {
          Api.del('/api/employees/' + id).then(function(r) {
            if (!r.success) { UI.toast(r.message, 'error'); return; }
            load();
          });
        });
    };

    window.empImport = function() {
        UI.modal('从 Excel 导入人员',
            '<div class="alert alert-info">' +
                '<strong>导入步骤</strong><br>' +
                '1. 点击「下载模板」获取 Excel 文件<br>' +
                '2. 按模板格式填写人员信息<br>' +
                '3. 选择填好的文件后点击「开始导入」<br><br>' +
                '<strong>模板字段:</strong> 姓名*（必填）、性别、身份证号、部门名称*（需与系统中已有部门一致）、职务、参加工作时间*（YYYY-MM-DD）、电话、备注' +
            '</div>' +
            '<div class="form-group">' +
                '<label>步骤 1: 下载 Excel 模板</label><br>' +
                '<button class="btn btn-secondary" onclick="UI.downloadExcel(\'/api/import/employees/template\')">下载模板</button>' +
            '</div>' +
            '<div class="form-group">' +
                '<label>步骤 2: 选择已填好的 Excel 文件</label>' +
                '<input type="file" id="import_file" accept=".xlsx,.xls" style="width:100%;">' +
            '</div>' +
            '<div id="import_result" style="display:none;"></div>',
            function(form) {
                var fileInput = form.querySelector('#import_file');
                if (!fileInput.files.length) { UI.toast('请先选择文件', 'error'); throw new Error(); }
                var fd = new FormData();
                fd.append('file', fileInput.files[0]);
                var resultEl = form.querySelector('#import_result');
                resultEl.style.display = 'block';
                resultEl.className = 'alert alert-info';
                resultEl.textContent = '导入中, 请稍候...';
                return Api.postForm('/api/import/employees', fd).then(function(r) {
                    var html = '';
                    if (r.success) {
                        html = '<div style="color:var(--success);"><strong>导入完成</strong><br>' +
                               '总数 ' + r.total + ' 行 · 成功 ' + r.success + ' 行 · 失败 ' + r.failed + ' 行</div>';
                        if (r.details && r.details.length > 0) {
                            html += '<hr style="margin:10px 0;border:none;border-top:1px solid var(--border);"><div style="max-height:200px;overflow-y:auto;font-size:12px;line-height:1.8;"><strong>明细:</strong><br>';
                            for (var i = 0; i < r.details.length; i++) html += r.details[i] + '<br>';
                            html += '</div>';
                        }
                        resultEl.className = 'alert alert-success';
                        resultEl.innerHTML = html;
                        setTimeout(function() { UI.closeModal(); load(); }, 3000);
                    } else {
                        resultEl.className = 'alert alert-danger';
                        resultEl.textContent = r.message || '导入失败';
                        throw new Error();
                    }
                }).catch(function(e) {
                    resultEl.className = 'alert alert-danger';
                    resultEl.textContent = '导入失败: ' + (e.message || e);
                    throw e;
                });
            },
            '开始导入'
        );
    };
})();
