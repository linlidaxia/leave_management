// 用户管理
(function() {
    var isAdmin = false;
    var pageSize = 15;
    var currentPage = 0;

    Auth.requireAuth().then(function(user) {
        if (!user) return;
        if (Auth.user.role !== 'ADMIN') {
            document.getElementById('contentArea').innerHTML = '<div class="alert alert-danger">无权访问此页面</div>';
            return;
        }
        isAdmin = true;
        return load();
    }).catch(function(e) { console.error(e); });

    function load() {
        return Api.get('/api/admin/users?page=' + currentPage + '&size=' + pageSize).then(function(resp) {
            var pageInfo = UI.parsePageData(resp);
            var list = pageInfo.rows;
            var roleOpts = '<option value="ADMIN">管理员</option><option value="USER">操作员</option><option value="VIEWER">查看员</option>';
            var rows = [];
            for (var i = 0; i < list.length; i++) {
                var u = list[i];
                rows.push([u.id, u.username, u.displayName||'—', u.role,
                    u.enabled ? '<span class="tag tag-approved">启用</span>' : '<span class="tag tag-pending">禁用</span>', u]);
            }
            var html =
                '<div class="page-header"><div><div class="page-title">用户管理</div><div class="page-subtitle">User Management</div></div></div>' +
                '<div class="toolbar"><button class="btn btn-gold" onclick="userAdd()">+ 添加用户</button> <button class="btn btn-secondary" onclick="load()">刷新</button></div>' +
                UI.table(['ID', '用户名', '显示名', '角色', '状态', '操作'], rows, [null,null,null,null,null, function(val, row) {
                    var u = row[5];
                    var btns = '<button class="btn btn-sm btn-primary" onclick="userResetPwd(' + u.id + ',\'' + u.username + '\')">重置密码</button> ';
                    btns += '<button class="btn btn-sm btn-warning" onclick="userToggle(' + u.id + ',' + (u.enabled ? 'false' : 'true') + ')">' + (u.enabled ? '禁用' : '启用') + '</button> ';
                    var roleSel = roleOpts.replace('value="' + u.role + '"', 'value="' + u.role + '" selected');
                    btns += '<select onchange="userRole(' + u.id + ',this.value)" style="padding:5px 8px;border:1px solid var(--border-2);border-radius:4px;background:var(--card);font-size:13px;height:30px;">' + roleSel + '</select> ';
                    if (u.id !== 1) btns += '<button class="btn btn-sm btn-danger" onclick="userDelete(' + u.id + ',\'' + u.username + '\')">删除</button>';
                    return btns;
                }]);
            document.getElementById('contentArea').innerHTML = html;
            var tw = document.querySelector('#contentArea .table-wrap');
            if (tw) tw.insertAdjacentHTML('afterend', UI.pagination(pageInfo.total, currentPage, pageSize, 'userChangePage'));
        });
    }
    window.load = load;

    window.userChangePage = function(p) { currentPage = p; load(); };

    window.userAdd = function() {
        UI.modal('添加用户',
            '<div class="form-group"><label>用户名 *</label><input id="u_username"></div>' +
            '<div class="form-group"><label>密码 *（至少 6 位）</label><input type="password" id="u_password"></div>' +
            '<div class="form-group"><label>显示名</label><input id="u_display"></div>' +
            '<div class="form-group"><label>角色</label><select id="u_role"><option value="USER">操作员</option><option value="ADMIN">管理员</option><option value="VIEWER">查看员</option></select></div>',
            function(form) {
                var username = form.querySelector('#u_username').value.trim();
                var password = form.querySelector('#u_password').value;
                if (!username) { UI.toast('请输入用户名', 'error'); throw new Error(); }
                if (password.length < 6) { UI.toast('密码至少 6 位', 'error'); throw new Error(); }
                return Api.post('/api/admin/users', {
                    username: username,
                    password: password,
                    displayName: form.querySelector('#u_display').value.trim(),
                    role: form.querySelector('#u_role').value
                }).then(function(r) {
                    if (r.success) { UI.closeModal(); load(); }
                    else { UI.toast(r.message, 'error'); throw new Error(); }
                });
            },
            '保存'
        );
    };

    window.userResetPwd = function(id, username) {
        UI.prompt('为「' + username + '」设置新密码（至少 6 位）', '', function(pwd) {
            if (!pwd || pwd.length < 6) { UI.toast('密码至少 6 位', 'error'); return; }
            Api.put('/api/admin/users/' + id + '/password', { password: pwd }).then(function(r) {
                if (r.success) UI.toast('密码已重置', 'success');
                else UI.toast(r.message, 'error');
            });
        });
    };

    window.userToggle = function(id, enable) {
        Api.put('/api/admin/users/' + id + '/profile', { displayName: '', enabled: enable === true || enable === 'true' }).then(function(r) {
            if (r.success) load(); else UI.toast(r.message, 'error');
        });
    };

    window.userRole = function(id, role) {
        Api.put('/api/admin/users/' + id + '/role', { role: role }).then(function(r) {
            if (!r.success) UI.toast(r.message, 'error');
        });
    };

    window.userDelete = function(id, username) {
        UI.confirm('确定要删除用户「' + username + '」吗?', function() {
            Api.del('/api/admin/users/' + id).then(function(r) {
                if (r.success) load(); else UI.toast(r.message, 'error');
            });
        });
    };
})();
