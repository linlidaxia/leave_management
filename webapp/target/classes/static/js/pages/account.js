// 修改密码
(function() {
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        document.getElementById('contentArea').innerHTML =
            '<div class="page-header"><div><div class="page-title">修改密码</div><div class="page-subtitle">Change Password</div></div></div>' +
            '<div class="card" >' +
                '<div class="form-group"><label>原密码</label><input type="password" id="acc_old"></div>' +
                '<div class="form-group"><label>新密码 （至少 6 位）</label><input type="password" id="acc_new"></div>' +
                '<div class="form-group"><label>确认新密码</label><input type="password" id="acc_confirm"></div>' +
                '<button class="btn btn-gold" onclick="accountSave()">提交修改</button>' +
            '</div>';
    }).catch(function(e) { console.error(e); });

    window.accountSave = function() {
        var old = document.getElementById('acc_old').value;
        var neu = document.getElementById('acc_new').value;
        var cfm = document.getElementById('acc_confirm').value;
        if (!old || !neu) { UI.toast('请填写完整', 'error'); return; }
        if (neu !== cfm) { UI.toast('两次输入的新密码不一致', 'error'); return; }
        if (neu.length < 6) { UI.toast('新密码至少 6 位', 'error'); return; }
        Api.post('/api/auth/change-password', { oldPassword: old, newPassword: neu }).then(function(r) {
            if (r.success) {
                UI.toast(r.message, 'success');
                document.getElementById('acc_old').value = '';
                document.getElementById('acc_new').value = '';
                document.getElementById('acc_confirm').value = '';
            } else {
                UI.toast(r.message, 'error');
            }
        });
    };
})();
