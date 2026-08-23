// 部门管理
(function() {
    var isAdmin = false;
    var pageSize = 15;
    var currentPage = 0;

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return load();
    }).catch(function(e) { console.error(e); });

    function load() {
        return Api.get('/api/departments?page=' + currentPage + '&size=' + pageSize).then(function(resp) {
            var pageInfo = UI.parsePageData(resp);
            var depts = pageInfo.rows;
            var rows = [];
            for (var i = 0; i < depts.length; i++) {
                var d = depts[i];
                rows.push([d.id, d.name, d.code || '—', d.sortOrder || 0, d.remark || '—', d]);
            }
            var html = '<div class="page-header"><div><div class="page-title">部门管理</div><div class="page-subtitle">Department Management</div></div></div>';
            html += '<div class="page-tip">提示: 点击列表中的「编辑」按钮可修改部门信息</div>';
            if (isAdmin) {
                html += '<div class="toolbar">' +
                    '<button class="btn btn-gold" onclick="deptEdit()">+ 添加部门</button> ' +
                    '<button class="btn btn-success" onclick="deptImport()">↓ 从 Excel 导入</button> ' +
                    '<button class="btn btn-secondary" onclick="load()">刷新</button>' +
                '</div>';
            }
            html += UI.table(
                ['ID', '部门名称', '编号', '排序', '备注', '操作'],
                rows,
                [null, null, null, null, null, function(val, row) {
                    var d = row[5];
                    if (!isAdmin) return '<span class="text-muted">只读</span>';
                    return '<button class="btn btn-sm btn-primary" onclick="deptEdit(' + d.id + ')">编辑</button> ' +
                           '<button class="btn btn-sm btn-danger" onclick="deptDelete(' + d.id + ',\'' + (d.name||'').replace(/'/g,"\\'") + '\')">删除</button>';
                }]
            );
            document.getElementById('contentArea').innerHTML = html;
            var tw = document.querySelector('#contentArea .table-wrap');
            if (tw) tw.insertAdjacentHTML('afterend', UI.pagination(pageInfo.total, currentPage, pageSize, 'deptChangePage'));
        });
    }
    window.load = load;

    window.deptChangePage = function(p) { currentPage = p; load(); };

    window.deptEdit = function(id) {
        var initData = { name: '', code: '', sortOrder: 0, remark: '' };
        var p = id ? Api.get('/api/departments').then(function(list) {
            for (var i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
            return initData;
        }) : Promise.resolve(initData);

        p.then(function(d) {
            UI.modal(id ? '编辑部门' : '添加部门',
                '<div class="form-group"><label>部门名称 *</label><input id="d_name" value="' + (d.name||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="form-group"><label>部门编号</label><input id="d_code" value="' + (d.code||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="form-group"><label>排序</label><input type="number" id="d_sort" value="' + (d.sortOrder||0) + '"></div>' +
                '<div class="form-group"><label>备注</label><input id="d_remark" value="' + (d.remark||'').replace(/"/g,'&quot;') + '"></div>',
                function(form) {
                    var name = form.querySelector('#d_name').value.trim();
                    if (!name) { UI.toast('请输入部门名称', 'error'); throw new Error(); }
                    return Api.post('/api/departments', {
                        id: id || null, name: name,
                        code: form.querySelector('#d_code').value.trim(),
                        sortOrder: parseInt(form.querySelector('#d_sort').value) || 0,
                        remark: form.querySelector('#d_remark').value.trim()
                    }).then(function(r) {
                        if (!r.success) { UI.toast(r.message, 'error'); throw new Error(); }
                        UI.closeModal(); load();
                    });
                },
                '保存'
            );
        });
    };

    window.deptDelete = function(id, name) {
        UI.confirm('确定要删除部门「' + name + '」吗?', function() {
          Api.del('/api/departments/' + id).then(function(r) {
            if (!r.success) { UI.toast(r.message, 'error'); return; }
            load();
          });
        });
    };

    window.deptImport = function() {
        UI.modal('从 Excel 导入部门',
            '<div class="alert alert-info">' +
                '<strong>导入步骤</strong><br>' +
                '1. 点击「下载模板」获取 Excel 文件<br>' +
                '2. 按模板格式填写部门信息<br>' +
                '3. 选择填好的文件后点击「开始导入」<br><br>' +
                '<strong>模板字段:</strong> 部门名称*（必填）、部门编号、排序、备注' +
            '</div>' +
            '<div class="form-group">' +
                '<label>步骤 1: 下载 Excel 模板</label><br>' +
                '<button class="btn btn-secondary" onclick="UI.downloadExcel(\'/api/import/departments/template\')">下载模板</button>' +
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
                return Api.postForm('/api/import/departments', fd).then(function(r) {
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
