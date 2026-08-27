// 假别维护
(function() {
    var isAdmin = false;
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        isAdmin = !Auth.isViewer();
        return load();
    }).catch(function(e) { console.error(e); });

    function load() {
        return Api.get('/api/leave-types').then(function(list) {
            var rows = [];
            for (var i = 0; i < list.length; i++) {
                var lt = list[i];
                var needAtt = lt.needAttachment
                    ? '<span class="tag tag-pending">需佐证</span>'
                    : '<span class="text-muted">—</span>';
                var deductAnnual = lt.deductFromAnnual
                    ? '<span class="tag tag-pending">扣公休</span>'
                    : '<span class="text-muted">—</span>';
                rows.push([lt.id, lt.name, lt.code || '—', lt.sortOrder || 0, needAtt, deductAnnual, lt.remark || '—', lt]);
            }
            var html = '<div class="page-header"><div><div class="page-title">假别维护</div><div class="page-subtitle">Leave Type Management</div></div></div>';
            html += '<div class="page-tip">提示: 勾选「需佐证」后，请假登记时该假别会要求上传 PDF 或图片附件；已被请假记录引用的假别无法删除</div>';
            if (isAdmin) {
                html += '<div class="toolbar">' +
                    '<button class="btn btn-gold" onclick="ltEdit()">+ 添加假别</button> ' +
                    '<button class="btn btn-secondary" onclick="load()">刷新</button>' +
                '</div>';
            }
            html += UI.table(
                ['ID', '假别名称', '编号', '排序', '需佐证', '扣公休', '备注', '操作'],
                rows,
                [null, null, null, null, null, null, null, function(val, row) {
                    var lt = row[7];
                    if (!isAdmin) return '<span class="text-muted">只读</span>';
                    var btns = '<button class="btn btn-sm btn-primary" onclick="ltEdit(' + lt.id + ')">编辑</button>';
                    // 公休假(ID=1)/事假(ID=2) 系统内置, 不允许删除
                    if (lt.id !== 1 && lt.id !== 2) {
                        btns += ' <button class="btn btn-sm btn-danger" onclick="ltDelete(' + lt.id + ',\'' + (lt.name||'').replace(/'/g,"\\'") + '\')">删除</button>';
                    } else {
                        btns += ' <span class="text-muted" style="font-size:11px;">系统内置</span>';
                    }
                    return btns;
                }]
            );
            document.getElementById('contentArea').innerHTML = html;
        });
    }
    window.load = load;

    window.ltEdit = function(id) {
        var initData = { name: '', code: '', sortOrder: 0, needAttachment: false, deductFromAnnual: false, remark: '' };
        var p = id ? Api.get('/api/leave-types').then(function(list) {
            for (var i = 0; i < list.length; i++) if (list[i].id === id) return list[i];
            return initData;
        }) : Promise.resolve(initData);

        p.then(function(lt) {
            UI.modal(id ? '编辑假别' : '添加假别',
                '<div class="form-row">' +
                    '<div class="form-group"><label>假别名称 *</label><input id="lt_name" value="' + (lt.name||'').replace(/"/g,'&quot;') + '"></div>' +
                    '<div class="form-group"><label>编号 *</label><input id="lt_code" value="' + (lt.code||'').replace(/"/g,'&quot;') + '" placeholder="如 GXJ/SHIJ"></div>' +
                '</div>' +
                '<div class="form-row">' +
                    '<div class="form-group"><label>排序</label><input type="number" id="lt_sort" value="' + (lt.sortOrder||0) + '" placeholder="数字越小越靠前"></div>' +
                    '<div class="form-group"><label>需佐证附件</label><select id="lt_need">' +
                        '<option value="false"' + (!lt.needAttachment ? ' selected' : '') + '>不需要</option>' +
                        '<option value="true"' + (lt.needAttachment ? ' selected' : '') + '>需要</option>' +
                    '</select></div>' +
                '</div>' +
                '<div class="form-row">' +
                    '<div class="form-group"><label>优先扣除公休</label><select id="lt_deduct">' +
                        '<option value="false"' + (!lt.deductFromAnnual ? ' selected' : '') + '>否</option>' +
                        '<option value="true"' + (lt.deductFromAnnual ? ' selected' : '') + '>是</option>' +
                    '</select></div>' +
                    '<div class="form-group"></div>' +
                '</div>' +
                '<div class="form-group"><label>备注</label><input id="lt_remark" value="' + (lt.remark||'').replace(/"/g,'&quot;') + '" placeholder="如规则说明"></div>',
                function(form) {
                    var name = form.querySelector('#lt_name').value.trim();
                    var code = form.querySelector('#lt_code').value.trim();
                    if (!name) { alert('请输入假别名称'); throw new Error(); }
                    if (!code) { alert('请输入假别编号'); throw new Error(); }
                    return Api.post('/api/leave-types', {
                        id: id || null, name: name, code: code,
                        sortOrder: parseInt(form.querySelector('#lt_sort').value) || 0,
                        needAttachment: form.querySelector('#lt_need').value === 'true',
                        deductFromAnnual: form.querySelector('#lt_deduct').value === 'true',
                        remark: form.querySelector('#lt_remark').value.trim()
                    }).then(function(r) {
                        if (!r.success) { UI.toast(r.message, 'error'); throw new Error(); }
                        UI.closeModal(); load();
                    });
                },
                '保存'
            );
        });
    };

    window.ltDelete = function(id, name) {
        UI.confirm('确定要删除假别「' + name + '」吗?', function() {
          Api.del('/api/leave-types/' + id).then(function(r) {
            if (!r.success) { UI.toast(r.message, 'error'); return; }
            load();
          });
        });
    };

})();
