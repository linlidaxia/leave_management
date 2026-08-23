// 请假记录列表 (独立页: 查询条件 + 列表 + 导出 Excel + 附件管理)
(function() {
    var isAdmin = false;
    var depts = [];
    var lts = [];
    var emps = [];
    var identities = [];
    var pageSize = 15;
    var currentPage = 0;

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
        return load();
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

    function buildStatusOpts() {
        return '<option value="">全部</option><option value="待审批">待审批</option><option value="已审批">已审批</option><option value="已销假">已销假</option>';
    }

    function load() {
        var year = document.getElementById('ll_year') ? document.getElementById('ll_year').value : '';
        var deptId = document.getElementById('ll_dept') ? document.getElementById('ll_dept').value : '';
        var empId = document.getElementById('ll_emp') ? document.getElementById('ll_emp').value : '';
        var ltId = document.getElementById('ll_lt') ? document.getElementById('ll_lt').value : '';
        var identityId = document.getElementById('ll_identity') ? document.getElementById('ll_identity').value : '';
        var status = document.getElementById('ll_status') ? document.getElementById('ll_status').value : '';
        var sd = document.getElementById('ll_sd') ? document.getElementById('ll_sd').value : '';
        var ed = document.getElementById('ll_ed') ? document.getElementById('ll_ed').value : '';

        var params = [];
        if (year) params.push('year=' + year);
        if (deptId) params.push('deptId=' + deptId);
        if (empId) params.push('employeeId=' + empId);
        if (ltId) params.push('leaveTypeId=' + ltId);
        if (identityId) params.push('identityId=' + identityId);
        if (status) params.push('status=' + encodeURIComponent(status));
        if (sd) params.push('startDate=' + sd);
        if (ed) params.push('endDate=' + ed);
        params.push('page=' + currentPage);
        params.push('size=' + pageSize);
        var url = '/api/applications' + (params.length ? '?' + params.join('&') : '');

        return Api.get(url).then(function(resp) {
            var pageInfo = UI.parsePageData(resp);
            var apps = pageInfo.rows;
            var rows = [];
            for (var i = 0; i < apps.length; i++) {
                var a = apps[i];
                var canCheck = isAdmin && a.status === '待审批';
                rows.push([
                    '<input type="checkbox" class="ll-check" data-id="' + a.id + '"' + (canCheck ? '' : ' disabled>') ,
                    a.id,
                    a.employeeName,
                    a.departmentName || '—',
                    a.leaveTypeName,
                    (a.startDate || '').substring(0, 10) + ' ' + a.startPeriod,
                    (a.endDate || '').substring(0, 10) + ' ' + a.endPeriod,
                    UI.fmtNum(a.days, 1),
                    a.reason || '—',
                    UI.statusTag(a.status),
                    (a.applyDate || '').substring(0, 10),
                    (a.approveDate || '').substring(0, 10),
                    a,
                    a.id
                ]);
            }

            var html =
                '<div class="page-header">' +
                    '<div><div class="page-title">请假记录</div><div class="page-subtitle">Leave Application Records</div></div>' +
                '</div>' +
                '<div class="page-tip">提示: 支持按年度/部门/人员/假别/状态/日期范围查询，可导出 Excel；附件管理点击附件数量</div>';

            // 查询条件区
            html += '<div class="card" style="padding:8px 12px;">';
            html += '<div class="form-row" style="gap:6px;flex-wrap:wrap;">';
            html += '<div class="form-group" style="width:80px;flex-shrink:0;"><label>年度</label><input type="number" id="ll_year" value="' + (year || UI.currentYear()) + '"></div>';
            html += '<div class="form-group" style="width:140px;flex-shrink:0;"><label>部门</label><select id="ll_dept" onchange="onLeaveListDeptChange()">' + buildDeptOpts() + '</select></div>';
            html += '<div class="form-group" style="width:140px;flex-shrink:0;"><label>人员</label><select id="ll_emp">' + buildEmpOpts(deptId) + '</select></div>';
            html += '<div class="form-group" style="width:120px;flex-shrink:0;"><label>假别</label><select id="ll_lt">' + buildLtOpts() + '</select></div>';
            html += '<div class="form-group" style="width:120px;flex-shrink:0;"><label>身份</label><select id="ll_identity">' + buildIdentityOpts() + '</select></div>';
            html += '<div class="form-group" style="width:100px;flex-shrink:0;"><label>状态</label><select id="ll_status">' + buildStatusOpts() + '</select></div>';
            html += '</div>';
            html += '<div class="form-row" style="gap:6px;margin-top:4px;flex-wrap:wrap;">';
            html += '<div class="form-group" style="width:130px;flex-shrink:0;"><label>开始日期</label><input type="date" id="ll_sd" value="' + (sd || '') + '"></div>';
            html += '<div class="form-group" style="width:130px;flex-shrink:0;"><label>结束日期</label><input type="date" id="ll_ed" value="' + (ed || '') + '"></div>';
            html += '</div>';
            html += '<div style="display:flex;gap:6px;margin-top:6px;align-items:center;">';
            html += '<button class="btn btn-primary" onclick="load()">查询</button>';
            html += '<button class="btn btn-secondary" onclick="leaveListReset()">重置</button>';
            html += '<div style="flex:1;"></div>';
            if (isAdmin) {
                html += '<button class="btn btn-success" onclick="leaveListBatchApprove()">批量审批</button> ';
                html += '<button class="btn btn-danger" onclick="leaveListBatchDelete()">批量删除</button> ';
            }
            html += '<button class="btn btn-gold" onclick="leaveListExport()">导出 Excel</button>';
            if (isAdmin) {
                html += ' <button class="btn btn-secondary" onclick="leaveListImport()">导入历史记录</button>';
            }
            html += '</div>';
            html += '</div>';

            // 列表
            html += '<div class="card" style="padding:0;">';
            html += UI.table(
                ['<input type="checkbox" id="ll_check_all" onchange="leaveListToggleAll(this)">', 'ID', '姓名', '部门', '假别', '开始', '结束', '天数', '事由', '状态', '登记日期', '审批日期', '附件', '操作'],
                rows,
                [
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    // 附件列
                    function(val, row) {
                        var app = row[12];
                        var appId = app && app.id ? app.id : 0;
                        return '<span id="ll_attach_count_' + appId + '" data-app="' + appId + '">…</span>';
                    },
                    // 操作列
                    function(val, row) {
                        var a = row[12];
                        if (!isAdmin) return '<span class="text-muted">只读</span>';
                        var btns = '<button class="btn btn-sm btn-primary" onclick="leaveListEdit(' + a.id + ')">编辑</button>';
                        if (a.status === '待审批') btns += ' <button class="btn btn-sm btn-success" onclick="leaveListApprove(' + a.id + ')">审批</button>';
                        btns += ' <button class="btn btn-sm btn-danger" onclick="leaveListDelete(' + a.id + ')">删除</button>';
                        return btns;
                    }
                ]
            );
            html += '</div>';

            document.getElementById('contentArea').innerHTML = html;
            if (year) document.getElementById('ll_year').value = year;
            if (deptId) { document.getElementById('ll_dept').value = deptId; document.getElementById('ll_emp').innerHTML = buildEmpOpts(deptId); }
            if (empId) document.getElementById('ll_emp').value = empId;
            if (ltId) document.getElementById('ll_lt').value = ltId;
            if (identityId) document.getElementById('ll_identity').value = identityId;
            if (status) document.getElementById('ll_status').value = status;
            if (sd) document.getElementById('ll_sd').value = sd;
            if (ed) document.getElementById('ll_ed').value = ed;

            // 分页控件
            var listDiv = document.querySelector('#contentArea .card:last-child .table-wrap');
            if (listDiv) {
                listDiv.insertAdjacentHTML('afterend', UI.pagination(pageInfo.total, currentPage, pageSize, 'leaveListChangePage'));
            }
            refreshAllAttachCounts();
        });
    }
    window.load = load;

    window.leaveListChangePage = function(p) { currentPage = p; load(); };

    window.onLeaveListDeptChange = function() {
        var deptId = document.getElementById('ll_dept').value;
        var empSel = document.getElementById('ll_emp');
        if (empSel) empSel.innerHTML = buildEmpOpts(deptId);
    };

    window.leaveListReset = function() {
        document.getElementById('ll_year').value = UI.currentYear();
        document.getElementById('ll_dept').value = '';
        document.getElementById('ll_emp').value = '';
        document.getElementById('ll_lt').value = '';
        document.getElementById('ll_identity').value = '';
        document.getElementById('ll_status').value = '';
        document.getElementById('ll_sd').value = '';
        document.getElementById('ll_ed').value = '';
        load();
    };

    window.leaveListExport = function() {
        var year = document.getElementById('ll_year').value;
        var deptId = document.getElementById('ll_dept').value;
        var empId = document.getElementById('ll_emp').value;
        var ltId = document.getElementById('ll_lt').value;
        var status = document.getElementById('ll_status').value;
        var sd = document.getElementById('ll_sd').value;
        var ed = document.getElementById('ll_ed').value;
        var params = [];
        if (year) params.push('year=' + year);
        if (deptId) params.push('deptId=' + deptId);
        if (empId) params.push('employeeId=' + empId);
        if (ltId) params.push('leaveTypeId=' + ltId);
        if (status) params.push('status=' + encodeURIComponent(status));
        if (sd) params.push('startDate=' + sd);
        if (ed) params.push('endDate=' + ed);
        var url = '/api/applications/excel' + (params.length ? '?' + params.join('&') : '');
        UI.downloadExcel(url);
    };

    window.leaveListImport = function() {
        UI.modal('导入历史请假记录',
            '<div class="alert alert-info" style="font-size:12px;">' +
                '<b>导入说明:</b><br>' +
                '1. 先下载模板, 按格式填写请假记录<br>' +
                '2. 员工姓名和假别名称需与系统中已有数据一致<br>' +
                '3. 导入的历史记录状态默认为「已审批」<br>' +
                '4. 历史记录不会扣减年假额度' +
            '</div>' +
            '<div style="margin-bottom:10px;">' +
                '<button class="btn btn-secondary" onclick="leaveListDownloadTemplate()">下载导入模板</button>' +
            '</div>' +
            '<div class="form-group"><label>选择 Excel 文件</label>' +
            '<input type="file" id="ll_import_file" accept=".xlsx,.xls" style="font-size:12px;"></div>' +
            '<div style="margin-top:10px;"><button class="btn btn-primary" onclick="leaveListDoImport()">开始导入</button></div>' +
            '<div id="ll_import_result" style="margin-top:8px;"></div>',
            function() { UI.closeModal(); },
            '关闭'
        );
    };

    window.leaveListDownloadTemplate = function() {
        UI.downloadExcel('/api/import/applications/template');
    };

    window.leaveListDoImport = function() {
        var fileInput = document.getElementById('ll_import_file');
        if (!fileInput || !fileInput.files.length) {
            UI.toast('请先选择 Excel 文件', 'error');
            return;
        }
        var resultDiv = document.getElementById('ll_import_result');
        resultDiv.innerHTML = '<p class="text-muted" style="font-size:12px;">正在导入...</p>';
        var fd = new FormData();
        fd.append('file', fileInput.files[0]);
        Api.postForm('/api/import/applications', fd).then(function(r) {
            if (r.success) {
                var html = '<div class="alert alert-success" style="font-size:12px;">' +
                    '导入完成: 总计 ' + r.total + ' 条, 成功 <b>' + r.success + '</b> 条';
                if (r.failed > 0) html += ', 失败 <b style="color:var(--danger)">' + r.failed + '</b> 条';
                html += '</div>';
                if (r.errors && r.errors.length > 0) {
                    html += '<div style="max-height:150px;overflow-y:auto;font-size:11px;color:var(--danger);">';
                    for (var i = 0; i < r.errors.length; i++) html += r.errors[i] + '<br>';
                    html += '</div>';
                }
                resultDiv.innerHTML = html;
                if (r.failed === 0) load();
            } else {
                resultDiv.innerHTML = '<div class="alert alert-danger" style="font-size:12px;">' + (r.message || '导入失败') + '</div>';
            }
        }).catch(function(e) {
            resultDiv.innerHTML = '<div class="alert alert-danger" style="font-size:12px;">导入异常: ' + e.message + '</div>';
        });
    };

    function refreshAllAttachCounts() {
        var cells = document.querySelectorAll('[id^="ll_attach_count_"]');
        for (var i = 0; i < cells.length; i++) {
            (function(cell) {
                var appId = cell.getAttribute('data-app');
                Api.get('/api/applications/' + appId + '/attachments').then(function(list) {
                    if (list.length === 0) {
                        cell.innerHTML = '<span class="text-muted">0</span>';
                    } else {
                        cell.innerHTML = '<span style="color:var(--gold-dark);font-weight:600;cursor:pointer;" onclick="openAttachModal(' + appId + ')">' + list.length + ' 个</span>';
                    }
                }).catch(function() { cell.innerHTML = '<span class="text-muted">—</span>'; });
            })(cells[i]);
        }
    }

    // 编辑请假记录 (独立实现, 不依赖 apply.js)
    window.leaveListEdit = function(id) {
        Api.get('/api/applications').then(function(allApps) {
            var a = null;
            for (var i = 0; i < allApps.length; i++) if (allApps[i].id === id) a = allApps[i];
            if (!a) return;
            var empDeptId = null;
            for (var j = 0; j < emps.length; j++) {
                if (emps[j].id === a.employeeId) { empDeptId = emps[j].departmentId; break; }
            }
            var deptOpts = buildDeptOpts().replace('value="' + empDeptId + '"', 'value="' + empDeptId + '" selected');
            var empOpts = buildEmpOpts(empDeptId).replace('value="' + a.employeeId + '"', 'value="' + a.employeeId + '" selected');
            var ltOpts = buildLtOpts().replace('value="' + a.leaveTypeId + '"', 'value="' + a.leaveTypeId + '" selected');
            UI.modal('编辑请假记录',
                '<div class="form-row" style="gap:6px;">' +
                    '<div class="form-group"><label>部门</label><select id="le_dept" onchange="onLeaveEditDeptChange()" style="width:120px;flex-shrink:0;">' + deptOpts + '</select></div>' +
                    '<div class="form-group"><label>人员 *</label><select id="le_emp" style="width:120px;flex-shrink:0;">' + empOpts + '</select></div>' +
                    '<div class="form-group"><label>假别</label><select id="le_lt" style="width:100px;flex-shrink:0;">' + ltOpts + '</select></div>' +
                '</div>' +
                '<div class="form-row" style="gap:6px;">' +
                    '<div class="form-group"><label>开始日期</label><input type="date" id="le_sd" value="' + UI.fmtDate(a.startDate) + '" style="width:130px;flex-shrink:0;"></div>' +
                    '<div class="form-group"><label>开始时段</label><select id="le_sp" style="width:80px;flex-shrink:0;"><option ' + (a.startPeriod==='上午'?'selected':'') + '>上午</option><option ' + (a.startPeriod==='下午'?'selected':'') + '>下午</option></select></div>' +
                '</div>' +
                '<div class="form-row" style="gap:6px;">' +
                    '<div class="form-group"><label>结束日期</label><input type="date" id="le_ed" value="' + UI.fmtDate(a.endDate) + '" style="width:130px;flex-shrink:0;"></div>' +
                    '<div class="form-group"><label>结束时段</label><select id="le_ep" style="width:80px;flex-shrink:0;"><option ' + (a.endPeriod==='上午'?'selected':'') + '>上午</option><option ' + (a.endPeriod==='下午'?'selected':'') + '>下午</option></select></div>' +
                '</div>' +
                '<div class="form-group"><label>事由</label><input id="le_reason" value="' + (a.reason||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="alert alert-info" style="font-size:12px;">附件管理请关闭本对话框后，在列表中点击附件数量</div>',
                function(form) {
                    var sdVal = form.querySelector('#le_sd').value;
                    var edVal = form.querySelector('#le_ed').value;
                    if (edVal < sdVal) { UI.toast('结束日期不能早于开始日期', 'error'); return Promise.reject(new Error()); }
                    return Api.put('/api/applications/' + id, {
                        employeeId: parseInt(form.querySelector('#le_emp').value),
                        leaveTypeId: parseInt(form.querySelector('#le_lt').value),
                        startDate: sdVal,
                        startPeriod: form.querySelector('#le_sp').value,
                        endDate: edVal,
                        endPeriod: form.querySelector('#le_ep').value,
                        reason: form.querySelector('#le_reason').value
                    }).then(function(r) {
                        if (r.success) { UI.closeModal(); UI.toast('保存成功', 'success'); load(); }
                        else { UI.toast(r.message, 'error'); throw new Error(); }
                    });
                },
                '保存'
            );
        });
    };

    window.onLeaveEditDeptChange = function() {
        var deptId = document.getElementById('le_dept').value;
        var empSel = document.getElementById('le_emp');
        if (empSel) empSel.innerHTML = buildEmpOpts(deptId);
    };

    window.leaveListApprove = function(id) {
        UI.confirm('确认审批通过该请假记录?', function() {
            Api.post('/api/applications/' + id + '/approve', {}).then(function(r) {
                if (r.success) { UI.toast('审批通过', 'success'); load(); }
                else UI.toast(r.message, 'error');
            });
        });
    };

    window.leaveListDelete = function(id) {
        UI.confirm('确定要删除该请假记录吗? 如涉及事假抵扣公休假将恢复抵扣天数，同时删除关联附件。', function() {
            Api.del('/api/applications/' + id).then(function(r) {
                if (r.success) { UI.toast('已删除', 'success'); load(); }
                else UI.toast(r.message, 'error');
            });
        });
    };

    // 附件管理对话框 (被 leave-list 和 apply 共用)
    window.openAttachModal = function(appId) {
        UI.modal('附件管理 (请假记录 #' + appId + ')',
            '<div id="modal_attach_list" style="margin-bottom:12px;"></div>' +
            '<div class="alert alert-info" style="font-size:12px;">支持 PDF 或图片格式，单文件 ≤10MB，可多选</div>' +
            '<div class="flex gap-2 items-center">' +
                '<input type="file" id="modal_attach_file" accept=".pdf,.jpg,.jpeg,.png,.gif,.bmp,.webp,application/pdf,image/*" multiple style="font-size:12px;flex:1;">' +
                '<button class="btn btn-sm btn-gold" onclick="uploadAttachModal(' + appId + ')">上传</button>' +
            '</div>',
            function() { UI.closeModal(); },
            '关闭'
        );
        loadAttachList(appId);
    };

    function loadAttachList(appId) {
        var container = document.getElementById('modal_attach_list');
        if (!container) return;
        container.innerHTML = '<p class="text-muted" style="font-size:12px;">载入中...</p>';
        Api.get('/api/applications/' + appId + '/attachments').then(function(list) {
            if (list.length === 0) {
                container.innerHTML = '<p class="text-muted" style="font-size:12px;">暂无附件</p>';
                return;
            }
            var html = '<table class="data-table" style="font-size:12px;"><thead><tr><th>文件名</th><th>大小</th><th>上传时间</th><th>操作</th></tr></thead><tbody>';
            for (var i = 0; i < list.length; i++) {
                var at = list[i];
                html += '<tr><td>' + escapeHtml(at.fileName || '—') + '</td>' +
                    '<td>' + formatSize(at.fileSize) + '</td>' +
                    '<td>' + (at.uploadedAt || '—') + '</td>' +
                    '<td><button class="btn btn-sm btn-secondary" onclick="window.open(\'/api/attachments/' + at.id + '/preview\',\'_blank\')">预览</button> ' +
                    '<button class="btn btn-sm btn-primary" onclick="downloadAttach(' + at.id + ')">下载</button> ' +
                    '<button class="btn btn-sm btn-danger" onclick="deleteAttach(' + at.id + ', ' + appId + ')">删除</button></td></tr>';
            }
            html += '</tbody></table>';
            container.innerHTML = html;
        }).catch(function(e) {
            container.innerHTML = '<p class="hint-danger">加载失败: ' + escapeHtml(e.message) + '</p>';
        });
    }
    window.loadAttachList = loadAttachList;

    function escapeHtml(s) {
        if (s == null) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function formatSize(bytes) {
        if (!bytes) return '0 B';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1024 / 1024).toFixed(1) + ' MB';
    }

    window.downloadAttach = function(id) {
        var a = document.createElement('a');
        a.href = '/api/attachments/' + id + '/download';
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    };

    window.deleteAttach = function(id, appId) {
        UI.confirm('确定删除该附件吗?', function() {
            Api.del('/api/attachments/' + id).then(function(r) {
                if (r.success) loadAttachList(appId);
                else UI.toast(r.message || '删除失败', 'error');
            });
        });
    };

    window.uploadAttachModal = function(appId) {
        var fileInput = document.getElementById('modal_attach_file');
        if (!fileInput.files.length) { UI.toast('请先选择文件', 'error'); return; }
        var files = fileInput.files;
        var total = files.length;
        var success = 0, failed = 0;
        function uploadOne(idx) {
            if (idx >= total) {
                UI.toast('上传完成: 成功 ' + success + ' 个' + (failed > 0 ? ', 失败 ' + failed + ' 个' : ''), 'success');
                loadAttachList(appId);
                return;
            }
            var fd = new FormData();
            fd.append('file', files[idx]);
            var list = document.getElementById('modal_attach_list');
            list.innerHTML = '<p class="hint">正在上传 (' + (idx + 1) + '/' + total + '): ' + escapeHtml(files[idx].name) + '...</p>';
            Api.postForm('/api/applications/' + appId + '/attachments', fd).then(function(r) {
                if (r.success) success++;
                else failed++;
            }).catch(function() { failed++; }).then(function() { uploadOne(idx + 1); });
        }
        uploadOne(0);
    };

    // 全选/全不选
    window.leaveListToggleAll = function(el) {
        var boxes = document.querySelectorAll('.ll-check');
        for (var i = 0; i < boxes.length; i++) {
            if (!boxes[i].disabled) boxes[i].checked = el.checked;
        }
    };

    // 批量删除
    window.leaveListBatchDelete = function() {
        var boxes = document.querySelectorAll('.ll-check:checked');
        var ids = [];
        for (var i = 0; i < boxes.length; i++) ids.push(parseInt(boxes[i].getAttribute('data-id')));
        if (ids.length === 0) { UI.toast('请先勾选需要删除的记录', 'error'); return; }
        UI.confirm('确定要删除选中的 ' + ids.length + ' 条记录吗? 如涉及事假抵扣公休假将恢复抵扣天数，同时删除关联附件。', function() {
            Api.post('/api/applications/batch-delete', ids).then(function(r) {
                if (r.success) { UI.toast(r.message, 'success'); load(); }
                else UI.toast(r.message, 'error');
            });
        });
    };

    // 批量审批
    window.leaveListBatchApprove = function() {
        var boxes = document.querySelectorAll('.ll-check:checked');
        var ids = [];
        for (var i = 0; i < boxes.length; i++) ids.push(parseInt(boxes[i].getAttribute('data-id')));
        if (ids.length === 0) { UI.toast('请先勾选需要审批的记录', 'error'); return; }
        UI.confirm('确认批量审批选中的 ' + ids.length + ' 条记录?', function() {
            Api.post('/api/applications/batch-approve', ids).then(function(r) {
                if (r.success) { UI.toast(r.message, 'success'); load(); }
                else UI.toast(r.message, 'error');
            });
        });
    };
})();
