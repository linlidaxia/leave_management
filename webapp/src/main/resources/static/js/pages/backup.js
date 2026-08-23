// 数据备份
(function() {
    Auth.requireAuth().then(function(user) {
        if (!user) return;
        if (Auth.user.role !== 'ADMIN') {
            document.getElementById('contentArea').innerHTML = '<div class="alert alert-danger">无权访问此页面</div>';
            return;
        }
        return Api.get('/api/backup/info');
    }).then(function(info) {
        if (!info) return;
        var html =
            '<div class="page-header"><div><div class="page-title">数据备份</div><div class="page-subtitle">Data Backup & Restore</div></div></div>' +
            '<div class="card">' +
                '<h3>数据库信息</h3>' +
                '<div style="line-height:2.2;color:var(--text-2);font-size:13px;">' +
                    '<strong style="color:var(--ink);">数据库路径:</strong> <code style="background:var(--paper-2);padding:2px 8px;border-radius:3px;font-family:JetBrains Mono,monospace;font-size:12px;">' + info.dbPath + '</code><br>' +
                    '<strong style="color:var(--ink);">数据库格式:</strong> ' + info.dbType + '<br>' +
                    '<strong style="color:var(--ink);">文件大小:</strong> ' + (info.dbSize / 1024).toFixed(1) + ' KB<br>' +
                    '<strong style="color:var(--ink);">建议:</strong> 定期备份，防止数据丢失' +
                '</div>' +
            '</div>' +
            '<div class="card">' +
                '<h3>备份与恢复</h3>' +
                '<div class="flex gap-3 items-center flex-wrap">' +
                    '<button class="btn btn-gold" onclick="UI.downloadExcel(\'/api/backup/download\')">下载数据库备份</button>' +
                    '<form id="restoreForm" style="display:inline-flex;gap:10px;align-items:center;">' +
                        '<input type="file" id="restoreFile" accept=".db,.sqlite" required style="padding:6px;border:1px solid var(--border-strong);border-radius:3px;background:var(--paper);">' +
                        '<button class="btn btn-danger" type="button" onclick="backupRestore()">恢复数据库</button>' +
                    '</form>' +
                '</div>' +
            '</div>';
        document.getElementById('contentArea').innerHTML = html;
    }).catch(function(e) { console.error(e); });

    window.backupRestore = function() {
        var fileInput = document.getElementById('restoreFile');
        if (!fileInput.files.length) { UI.toast('请选择备份文件', 'error'); return; }
        UI.confirm('恢复将覆盖当前数据，是否继续?', function() {
        var fd = new FormData();
        fd.append('file', fileInput.files[0]);
        Api.postForm('/api/backup/restore', fd).then(function(r) {
            if (r.success) {
                UI.toast(r.message + ' 即将重新登录...', 'success');
                Auth.logout();
            } else {
                UI.toast(r.message, 'error');
            }
        }).catch(function(e) { UI.toast('恢复失败: ' + e.message, 'error'); });
      });
    };
})();
