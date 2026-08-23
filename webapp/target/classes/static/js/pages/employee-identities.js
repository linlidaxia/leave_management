// 人员身份管理
(function() {
    var isAdmin = false;

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return load();
    }).catch(function(e) { console.error(e); });

    function load() {
        return Api.get('/api/identities').then(function(list) {
            var rows = [];
            for (var i = 0; i < list.length; i++) {
                var ei = list[i];
                rows.push([
                    ei.id, ei.name, ei.code||'—', ei.sortOrder||0, ei.remark||'—', ei
                ]);
            }
            var html = '<div class="page-header"><div><div class="page-title">人员身份管理</div><div class="page-subtitle">Employee Identity Management</div></div></div>';
            html += '<div class="page-tip">提示: 人员身份用于区分员工类别(如干部、职工、合同工等), 可在人员管理中关联</div>';
            if (isAdmin) {
                html += '<div class="toolbar"><button class="btn btn-gold" onclick="eiEdit()">+ 添加身份</button></div>';
            }
            html += UI.table(
                ['ID', '身份名称', '编号', '排序', '备注', '操作'],
                rows,
                [null, null, null, null, null, function(val, row) {
                    var ei = row[5];
                    if (!isAdmin) return '<span class="text-muted">只读</span>';
                    return '<button class="btn btn-sm btn-primary" onclick="eiEdit(' + ei.id + ')">编辑</button> ' +
                           '<button class="btn btn-sm btn-danger" onclick="eiDelete(' + ei.id + ',\'' + (ei.name||'').replace(/'/g,"\\'") + '\')">删除</button>';
                }]
            );
            document.getElementById('contentArea').innerHTML = html;
        });
    }
    window.load = load;

    window.eiEdit = function(id) {
        var initData = { name: '', code: '', sortOrder: 0, remark: '' };
        var p = id ? Api.get('/api/identities').then(function(list) {
            for (var i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
            return initData;
        }) : Promise.resolve(initData);

        p.then(function(ei) {
            UI.modal(id ? '编辑身份' : '添加身份',
                '<div class="form-group"><label>身份名称 *</label><input id="ei_name" value="' + (ei.name||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="form-group"><label>编号</label><input id="ei_code" value="' + (ei.code||'').replace(/"/g,'&quot;') + '"></div>' +
                '<div class="form-group"><label>排序</label><input type="number" id="ei_sort" value="' + (ei.sortOrder||0) + '"></div>' +
                '<div class="form-group"><label>备注</label><input id="ei_remark" value="' + (ei.remark||'').replace(/"/g,'&quot;') + '"></div>',
                function(form) {
                    var name = form.querySelector('#ei_name').value.trim();
                    if (!name) { UI.toast('请输入身份名称', 'error'); throw new Error(); }
                    return Api.post('/api/identities', {
                        id: id || null,
                        name: name,
                        code: form.querySelector('#ei_code').value.trim(),
                        sortOrder: parseInt(form.querySelector('#ei_sort').value) || 0,
                        remark: form.querySelector('#ei_remark').value.trim()
                    }).then(function(r) {
                        if (!r.success) { UI.toast(r.message, 'error'); throw new Error(); }
                        UI.closeModal(); load();
                    });
                },
                '保存'
            );
        });
    };

    window.eiDelete = function(id, name) {
        UI.confirm('确定要删除身份「' + name + '」吗?', function() {
            Api.del('/api/identities/' + id).then(function(r) {
                if (!r.success) { UI.toast(r.message, 'error'); return; }
                load();
            });
        });
    };
})();
