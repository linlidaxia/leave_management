// 请假登记 (只负责登记表单; 列表见 leave-list.html)
(function() {
    var isAdmin = false;
    var allEmps = [];
    var allDepts = [];
    var allLts = [];

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return Promise.all([Api.get('/api/employees'), Api.get('/api/departments'), Api.get('/api/leave-types')]);
    }).then(function(results) {
        if (!results) return;
        allEmps = results[0];
        allDepts = results[1];
        allLts = results[2];
        return render();
    }).catch(function(e) { console.error(e); });

    function buildDeptOptions(selectedId) {
        var opts = ['<option value="">— 选择部门 —</option>'];
        for (var i = 0; i < allDepts.length; i++) {
            var d = allDepts[i];
            opts.push('<option value="' + d.id + '"' + (selectedId !== undefined && selectedId === d.id ? ' selected' : '') + '>' + d.name + '</option>');
        }
        return opts.join('');
    }

    function buildEmpOptions(deptId, selectedEmpId) {
        var opts = ['<option value="">— 选择人员 —</option>'];
        for (var i = 0; i < allEmps.length; i++) {
            var e = allEmps[i];
            if (deptId && e.departmentId !== parseInt(deptId)) continue;
            var sel = (selectedEmpId !== undefined && selectedEmpId === e.id) ? ' selected' : '';
            opts.push('<option value="' + e.id + '"' + sel + '>' + e.name + (e.position ? ' (' + e.position + ')' : '') + '</option>');
        }
        if (opts.length === 1) opts.push('<option value="" disabled>该部门下暂无人员</option>');
        return opts.join('');
    }

    function buildLtOptions(selectedId) {
        var opts = ['<option value="">— 选择假别 —</option>'];
        for (var i = 0; i < allLts.length; i++) {
            var l = allLts[i];
            opts.push('<option value="' + l.id + '"' + (selectedId !== undefined && selectedId === l.id ? ' selected' : '') + '>' + l.name + '</option>');
        }
        return opts.join('');
    }

    // 查找当前选中的假别是否需要附件
    function currentLtNeedAttachment() {
        var sel = document.getElementById('a_lt');
        if (!sel) return false;
        var id = parseInt(sel.value);
        for (var i = 0; i < allLts.length; i++) {
            if (allLts[i].id === id) return !!allLts[i].needAttachment;
        }
        return false;
    }

    function render() {
        if (!isAdmin) {
            document.getElementById('contentArea').innerHTML =
                '<div class="page-header"><div><div class="page-title">请假登记</div><div class="page-subtitle">New Leave Application</div></div></div>' +
                '<div class="alert alert-danger">您没有权限操作此页面</div>';
            return;
        }

        var today = UI.today();
        var html = '';
        html += '<div class="page-header"><div><div class="page-title">请假登记</div><div class="page-subtitle">New Leave Application</div></div></div>';
        html += '<div class="page-tip">提示: 请先选择部门再选人员；选择事假时自动抵扣公休假, 半天按 0.5 天计算；标注「需佐证」的假别在表单内直接上传附件 (PDF/图片) ; 提交后可在 <a href="/leave-list.html" style="color:var(--gold-dark);font-weight:600;">请假记录</a> 中查看/补充附件</div>';

        html += '<div class="card" >' +
            '<div class="form-row" style="flex-wrap:wrap;">' +
                '<div class="form-group"><label>部门 *</label><select id="a_dept" onchange="onDeptChange()">' + buildDeptOptions() + '</select></div>' +
                '<div class="form-group"><label>人员 *</label><select id="a_emp">' + buildEmpOptions() + '</select></div>' +
                '<div class="form-group"><label>假别 *</label><select id="a_lt" onchange="onLtChange()">' + buildLtOptions() + '</select></div>' +
            '</div>' +
            '<div class="form-row" style="flex-wrap:wrap;">' +
                '<div class="form-group"><label>开始日期</label><input type="date" id="a_sd" value="' + today + '" onchange="applyCalcAuto()"></div>' +
                '<div class="form-group"><label>开始时段</label><select id="a_sp" onchange="applyCalcAuto()"><option>上午</option><option>下午</option></select></div>' +
                '<div class="form-group"><label>结束日期</label><input type="date" id="a_ed" value="' + today + '" onchange="applyCalcAuto()"></div>' +
                '<div class="form-group"><label>结束时段</label><select id="a_ep" onchange="applyCalcAuto()"><option>上午</option><option selected>下午</option></select></div>' +
            '</div>' +
            '<div class="form-group"><label>事由</label><input id="a_reason" placeholder="请输入请假事由"></div>' +
            '<div class="hint" id="a_calc" style="margin-bottom:6px;"></div>' +
            '<div class="hint-danger" id="a_offset" style="margin-bottom:10px;"></div>' +
            // 附件上传区 (按需显示)
            '<div id="a_attach_zone" style="display:none;margin-bottom:12px;padding:10px 12px;background:var(--paper-2);border:1px dashed var(--border-strong);border-radius:3px;">' +
                '<label style="display:block;margin-bottom:5px;color:var(--gold-dark);font-size:12.5px;font-weight:600;">📎 佐证附件 (该假别需提供 PDF 或图片)</label>' +
                '<div id="a_attach_list" style="margin-bottom:6px;"></div>' +
                '<input type="file" id="a_attach_file" accept=".pdf,.jpg,.jpeg,.png,.gif,.bmp,.webp,application/pdf,image/*" multiple style="font-size:12px;">' +
                '<div class="hint" style="margin-top:4px;">支持 PDF 或图片，可多选；单文件 ≤10MB；附件将在提交请假时一并上传</div>' +
            '</div>' +
            // 操作按钮
            '<div class="flex gap-2 flex-wrap">' +
                '<button class="btn btn-gold" onclick="applySubmit()">提交请假</button>' +
                '<button class="btn btn-secondary" onclick="applyReset()">重置</button>' +
            '</div>' +
        '</div>';

        document.getElementById('contentArea').innerHTML = html;
        updateAttachZoneVisibility();
    }
    window.render = render;

    // 自动计算 (日期/时段变化时触发, 不弹 alert)
    window.applyCalcAuto = function() {
        var empId = document.getElementById('a_emp').value;
        var ltId = document.getElementById('a_lt').value;
        var sd = document.getElementById('a_sd').value;
        var sp = document.getElementById('a_sp').value;
        var ed = document.getElementById('a_ed').value;
        var ep = document.getElementById('a_ep').value;
        if (!empId || !ltId) {
            document.getElementById('a_calc').textContent = '';
            document.getElementById('a_offset').textContent = '';
            return;
        }
        if (!sd || !ed) {
            document.getElementById('a_calc').textContent = '';
            document.getElementById('a_offset').textContent = '';
            return;
        }
        Api.get('/api/applications/preview?empId=' + empId + '&leaveTypeId=' + ltId +
                '&startDate=' + sd + '&startPeriod=' + encodeURIComponent(sp) +
                '&endDate=' + ed + '&endPeriod=' + encodeURIComponent(ep))
            .then(function(r) {
                document.getElementById('a_calc').textContent = '请假天数: ' + UI.fmtNum(r.days, 1) + ' 天';
                if (r.exceeds) {
                    var el = document.getElementById('a_calc');
                    el.style.color = 'var(--danger)';
                    el.style.fontWeight = '600';
                } else {
                    var el2 = document.getElementById('a_calc');
                    el2.style.color = '';
                    el2.style.fontWeight = '';
                }
                document.getElementById('a_offset').textContent = r.message || '';
            });
    };

    // 假别变化时切换附件区可见性 + 自动计算天数
    window.onLtChange = function() {
        updateAttachZoneVisibility();
        applyCalcAuto();
    };

    function updateAttachZoneVisibility() {
        var zone = document.getElementById('a_attach_zone');
        if (!zone) return;
        zone.style.display = currentLtNeedAttachment() ? 'block' : 'none';
    }

    window.onDeptChange = function() {
        var deptId = document.getElementById('a_dept').value;
        var empSel = document.getElementById('a_emp');
        if (empSel) {
            empSel.innerHTML = buildEmpOptions(deptId);
            applyCalcAuto();
        }
    };

    window.applySubmit = function() {
        var empId = document.getElementById('a_emp').value;
        var ltId = document.getElementById('a_lt').value;
        var sd = document.getElementById('a_sd').value;
        var sp = document.getElementById('a_sp').value;
        var ed = document.getElementById('a_ed').value;
        var ep = document.getElementById('a_ep').value;
        var reason = document.getElementById('a_reason').value;
        if (!empId || !ltId || !sd || !ed) { UI.toast('请选择人员/假别并填写日期', 'error'); return; }
        if (ed < sd) { UI.toast('结束日期不能早于开始日期', 'error'); return; }

        var needAtt = currentLtNeedAttachment();
        var fileInput = document.getElementById('a_attach_file');
        var pendingFiles = (needAtt && fileInput && fileInput.files.length > 0) ? fileInput.files : null;

        Api.post('/api/applications', {
            employeeId: parseInt(empId), leaveTypeId: parseInt(ltId),
            startDate: sd, startPeriod: sp, endDate: ed, endPeriod: ep, reason: reason
        }).then(function(r) {
            if (r.success) {
                var newAppId = r.id;
                if (pendingFiles && pendingFiles.length > 0) {
                    UI.toast('请假登记成功, 即将上传 ' + pendingFiles.length + ' 个附件...', 'success');
                    uploadFilesToApp(newAppId, pendingFiles, function() {
                        applyReset();
                    });
                } else {
                    if (needAtt) {
                        UI.toast(r.message + ' 该假别需附件, 请到「请假记录」中补充', 'info');
                    } else {
                        UI.toast(r.message, 'success');
                    }
                    applyReset();
                }
            } else UI.toast(r.message, 'error');
        });
    };

    function uploadFilesToApp(appId, files, onComplete) {
        var total = files.length;
        var success = 0, failed = 0;
        function uploadOne(idx) {
            if (idx >= total) {
                UI.toast('附件上传完成: 成功 ' + success + ' 个' + (failed > 0 ? ', 失败 ' + failed + ' 个' : ''), 'success');
                if (onComplete) onComplete();
                return;
            }
            var fd = new FormData();
            fd.append('file', files[idx]);
            Api.postForm('/api/applications/' + appId + '/attachments', fd).then(function(r) {
                if (r.success) success++;
                else failed++;
            }).catch(function() { failed++; }).then(function() { uploadOne(idx + 1); });
        }
        uploadOne(0);
    }

    window.applyReset = function() {
        document.getElementById('a_dept').value = '';
        document.getElementById('a_emp').innerHTML = buildEmpOptions();
        document.getElementById('a_lt').value = '';
        document.getElementById('a_sd').value = UI.today();
        document.getElementById('a_ed').value = UI.today();
        document.getElementById('a_sp').value = '上午';
        document.getElementById('a_ep').value = '下午';
        document.getElementById('a_reason').value = '';
        document.getElementById('a_attach_file').value = '';
        document.getElementById('a_calc').textContent = '';
        document.getElementById('a_offset').textContent = '';
        updateAttachZoneVisibility();
    };

    // 编辑请假记录 (被 leave-list.js 调用)
    window.applyEdit = function(id) {
        Api.get('/api/applications').then(function(allApps) {
            var a = null;
            for (var i = 0; i < allApps.length; i++) if (allApps[i].id === id) a = allApps[i];
            if (!a) return;
            var empDeptId = null;
            for (var j = 0; j < allEmps.length; j++) {
                if (allEmps[j].id === a.employeeId) { empDeptId = allEmps[j].departmentId; break; }
            }
            var deptOpts = buildDeptOptions(empDeptId);
            var empOpts = buildEmpOptions(empDeptId, a.employeeId);
            var ltOpts = buildLtOptions(a.leaveTypeId);
            UI.modal('编辑请假记录',
                '<div class="form-row" style="flex-wrap:wrap;">' +
                    '<div class="form-group"><label>部门</label><select id="ae_dept" onchange="onEditDeptChange()">' + deptOpts + '</select></div>' +
                    '<div class="form-group"><label>人员 *</label><select id="ae_emp">' + empOpts + '</select></div>' +
                    '<div class="form-group"><label>假别</label><select id="ae_lt">' + ltOpts + '</select></div>' +
                '</div>' +
                '<div class="form-row" style="flex-wrap:wrap;">' +
                    '<div class="form-group"><label>开始日期</label><input type="date" id="ae_sd" value="' + UI.fmtDate(a.startDate) + '"></div>' +
                    '<div class="form-group"><label>开始时段</label><select id="ae_sp"><option ' + (a.startPeriod==='上午'?'selected':'') + '>上午</option><option ' + (a.startPeriod==='下午'?'selected':'') + '>下午</option></select></div>' +
                '</div>' +
                '<div class="form-row" style="flex-wrap:wrap;">' +
                    '<div class="form-group"><label>结束日期</label><input type="date" id="ae_ed" value="' + UI.fmtDate(a.endDate) + '"></div>' +
                    '<div class="form-group"><label>结束时段</label><select id="ae_ep"><option ' + (a.endPeriod==='上午'?'selected':'') + '>上午</option><option ' + (a.endPeriod==='下午'?'selected':'') + '>下午</option></select></div>' +
                '</div>' +
                '<div class="form-group"><label>事由</label><input id="ae_reason" value="' + (a.reason||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="alert alert-info" style="font-size:12px;">附件管理请关闭本对话框后, 在「请假记录」中点击附件数量</div>',
                function(form) {
                    return Api.put('/api/applications/' + id, {
                        employeeId: parseInt(form.querySelector('#ae_emp').value),
                        leaveTypeId: parseInt(form.querySelector('#ae_lt').value),
                        startDate: form.querySelector('#ae_sd').value,
                        startPeriod: form.querySelector('#ae_sp').value,
                        endDate: form.querySelector('#ae_ed').value,
                        endPeriod: form.querySelector('#ae_ep').value,
                        reason: form.querySelector('#ae_reason').value
                    }).then(function(r) {
                        if (r.success) { UI.closeModal(); UI.toast('保存成功', 'success'); }
                        else { UI.toast(r.message, 'error'); throw new Error(); }
                    });
                },
                '保存'
            );
        });
    };

    window.onEditDeptChange = function() {
        var deptId = document.getElementById('ae_dept').value;
        var empSel = document.getElementById('ae_emp');
        if (empSel) empSel.innerHTML = buildEmpOptions(deptId);
    };

    // 附件管理对话框 (被 leave-list.js 调用)
    window.openAttachModal = function(appId) {
        UI.modal('附件管理 (请假记录 #' + appId + ')',
            '<div id="modal_attach_list" style="margin-bottom:12px;"></div>' +
            '<div class="alert alert-info" style="font-size:12px;">支持 PDF 或图片格式 (jpg/png/gif/bmp/webp), 单文件 ≤10MB, 可多选</div>' +
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
                var a = list[i];
                html += '<tr>' +
                    '<td>' + escapeHtml(a.fileName || '—') + '</td>' +
                    '<td>' + formatSize(a.fileSize) + '</td>' +
                    '<td>' + (a.uploadedAt || '—') + '</td>' +
                    '<td>' +
                        '<button class="btn btn-sm btn-secondary" onclick="window.open(\'/api/attachments/' + a.id + '/preview\',\'_blank\')">预览</button> ' +
                        '<button class="btn btn-sm btn-primary" onclick="downloadAttach(' + a.id + ')">下载</button> ' +
                        '<button class="btn btn-sm btn-danger" onclick="deleteAttach(' + a.id + ', ' + appId + ')">删除</button>' +
                    '</td>' +
                '</tr>';
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
                else { failed++; }
            }).catch(function() { failed++; }).then(function() { uploadOne(idx + 1); });
        }
        uploadOne(0);
    };
})();
