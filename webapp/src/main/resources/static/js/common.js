// ==================== 共享工具 (所有页面引用) ====================
// 兼容旧浏览器 - 纯 ES5 语法

// ---------- API 封装 ----------
var Api = {
    get: function(url) { return Api.req(url, { method: 'GET' }); },
    post: function(url, body) {
        return Api.req(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body || {})
        });
    },
    put: function(url, body) {
        return Api.req(url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body || {})
        });
    },
    del: function(url) { return Api.req(url, { method: 'DELETE' }); },
    postForm: function(url, formData) {
        return Api.req(url, { method: 'POST', body: formData });
    },
    req: function(url, opts) {
        opts = opts || {};
        opts.credentials = 'same-origin';
        return fetch(url, opts).then(function(resp) {
            if (resp.status === 401) {
                window.location.href = '/login.html';
                throw new Error('未登录或会话已过期');
            }
            if (resp.status === 403) {
                throw new Error('没有权限执行此操作');
            }
            var ct = resp.headers.get('content-type') || '';
            if (ct.indexOf('application/octet-stream') >= 0 ||
                ct.indexOf('spreadsheetml') >= 0 ||
                ct.indexOf('application/vnd.ms-excel') >= 0) {
                return resp.blob().then(function(blob) {
                    var filename = 'download.bin';
                    var cd = resp.headers.get('content-disposition') || '';
                    var m1 = cd.match(/filename\*=UTF-8''([^;]+)/i);
                    if (m1) filename = decodeURIComponent(m1[1]);
                    else {
                        var m2 = cd.match(/filename="([^"]+)"/i);
                        if (m2) filename = m2[1];
                    }
                    return { _download: true, blob: blob, filename: filename };
                });
            }
            return resp.text().then(function(text) {
                if (!text) return null;
                try { return JSON.parse(text); }
                catch (e) { return text; }
            });
        });
    },
    download: function(result) {
        var url = URL.createObjectURL(result.blob);
        var a = document.createElement('a');
        a.href = url; a.download = result.filename;
        document.body.appendChild(a); a.click(); document.body.removeChild(a);
        setTimeout(function() { URL.revokeObjectURL(url); }, 1000);
    }
};

// ---------- UI 工具 ----------
var UI = {
    table: function(headers, rows, renderers) {
        var html = '<div class="table-wrap"><table class="data-table"><thead><tr>';
        for (var i = 0; i < headers.length; i++) html += '<th>' + headers[i] + '</th>';
        html += '</tr></thead><tbody>';
        if (!rows || rows.length === 0) {
            html += '<tr><td colspan="' + headers.length + '" class="empty-tip">— 暂无数据 —</td></tr>';
        } else {
            for (var r = 0; r < rows.length; r++) {
                var row = rows[r];
                html += '<tr>';
                for (var j = 0; j < headers.length; j++) {
                    var val = row[j] !== undefined ? row[j] : '';
                    var fn = renderers ? renderers[j] : null;
                    html += '<td>' + (fn ? fn(val, row) : val) + '</td>';
                }
                html += '</tr>';
            }
        }
        html += '</tbody></table></div>';
        return html;
    },
    modal: function(title, bodyHtml, onOk, okText) {
        var root = document.getElementById('modalRoot');
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.innerHTML =
            '<div class="modal"><div class="modal-title"></div>' +
            '<div class="modal-body"></div>' +
            '<div class="modal-actions">' +
            '<button class="btn btn-secondary btn-cancel">取消</button>' +
            '<button class="btn btn-gold btn-ok"></button></div></div>';
        overlay.querySelector('.modal-title').textContent = title;
        var bodyDiv = overlay.querySelector('.modal-body');
        bodyDiv.innerHTML = bodyHtml;
        overlay.querySelector('.btn-ok').textContent = okText || '保存';
        var okBtn = overlay.querySelector('.btn-ok');
        okBtn.onclick = function() {
            okBtn.disabled = true;
            Promise.resolve(onOk(bodyDiv)).then(
                function() { /* ok */ },
                function(e) { okBtn.disabled = false; if (e && e.message) console.warn(e.message); }
            );
        };
        overlay.querySelector('.btn-cancel').onclick = function() { root.innerHTML = ''; };
        overlay.onclick = function(e) { if (e.target === overlay) root.innerHTML = ''; };
        root.innerHTML = '';
        root.appendChild(overlay);
        return bodyDiv;
    },
    closeModal: function() { document.getElementById('modalRoot').innerHTML = ''; },
    fmtDate: function(d) { return !d ? '' : String(d).substring(0, 10); },
    fmtNum: function(n, dec) {
        if (n === null || n === undefined || n === '') return '-';
        var v = parseFloat(n);
        if (isNaN(v)) return '-';
        return v.toFixed(dec === undefined ? 1 : dec);
    },
    statusTag: function(s) {
        if (s === '待审批') return '<span class="tag tag-pending">待审批</span>';
        if (s === '已审批') return '<span class="tag tag-approved">已审批</span>';
        if (s === '已销假') return '<span class="tag tag-cancelled">已销假</span>';
        return '<span class="tag">' + (s || '') + '</span>';
    },
    today: function() {
        var d = new Date();
        return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    },
    currentYear: function() { return new Date().getFullYear(); },
    downloadExcel: function(url) {
        Api.get(url).then(function(r) {
            if (r && r._download) Api.download(r);
        }).catch(function(e) { UI.toast('导出失败: ' + e.message, 'error'); });
    },

    // ---------- 分页控件 ----------
    pagination: function(total, page, size, onPageChange) {
        var totalPages = Math.ceil(total / size) || 1;
        if (totalPages <= 1) return '';
        var html = '<div class="pagination" style="display:flex;align-items:center;justify-content:center;gap:4px;padding:10px 0;">';
        // 上一页
        html += '<button class="btn btn-sm btn-secondary"' + (page === 0 ? ' disabled' : '') + ' onclick="' + (page > 0 ? onPageChange + '(' + (page - 1) + ')' : '') + '">上一页</button>';
        // 页码
        var start = Math.max(0, page - 2);
        var end = Math.min(totalPages, start + 5);
        if (start > 0) html += '<span class="text-muted" style="padding:0 4px;">…</span>';
        for (var i = start; i < end; i++) {
            if (i === page) {
                html += '<button class="btn btn-sm btn-gold"' + '>' + (i + 1) + '</button>';
            } else {
                html += '<button class="btn btn-sm btn-secondary" onclick="' + onPageChange + '(' + i + ')">' + (i + 1) + '</button>';
            }
        }
        if (end < totalPages) html += '<span class="text-muted" style="padding:0 4px;">…</span>';
        // 下一页
        html += '<button class="btn btn-sm btn-secondary"' + (page >= totalPages - 1 ? ' disabled' : '') + ' onclick="' + (page < totalPages - 1 ? onPageChange + '(' + (page + 1) + ')' : '') + '">下一页</button>';
        // 总数
        html += '<span class="text-muted" style="margin-left:8px;font-size:12px;">共 ' + total + ' 条 / ' + totalPages + ' 页</span>';
        html += '</div>';
        return html;
    },

    // ---------- 解析分页响应 ----------
    parsePageData: function(resp) {
        if (resp && resp.data && typeof resp.total === 'number') {
            return { rows: resp.data, total: resp.total, page: resp.page, size: resp.size, totalPages: resp.totalPages };
        }
        return { rows: resp || [], total: (resp || []).length, page: 0, size: 0, totalPages: 1 };
    },

    // ---------- 自定义通知 (Toast) ----------
    toast: function(msg, type) {
        type = type || 'info';
        // 移除旧 toast
        var old = document.getElementById('app-toast');
        if (old) old.remove();
        var el = document.createElement('div');
        el.id = 'app-toast';
        var bg, iconSvg;
        if (type === 'success') {
            bg = 'var(--success)';
            iconSvg = '<svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="flex-shrink:0;"><circle cx="10" cy="10" r="9" fill="rgba(255,255,255,0.25)"/><path d="M6 10.5L9 13.5L14.5 7.5" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
        } else if (type === 'error') {
            bg = 'var(--danger)';
            iconSvg = '<svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="flex-shrink:0;"><circle cx="10" cy="10" r="9" fill="rgba(255,255,255,0.25)"/><path d="M7 7L13 13M13 7L7 13" stroke="white" stroke-width="2.2" stroke-linecap="round"/></svg>';
        } else if (type === 'warning') {
            bg = 'var(--warning)';
            iconSvg = '<svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="flex-shrink:0;"><circle cx="10" cy="10" r="9" fill="rgba(255,255,255,0.25)"/><path d="M10 6V11M10 13.5V14" stroke="white" stroke-width="2.2" stroke-linecap="round"/></svg>';
        } else {
            bg = 'var(--info)';
            iconSvg = '<svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="flex-shrink:0;"><circle cx="10" cy="10" r="9" fill="rgba(255,255,255,0.25)"/><path d="M10 9V14M10 6.5V7" stroke="white" stroke-width="2.2" stroke-linecap="round"/></svg>';
        }
        el.style.cssText =
            'position:fixed;top:20px;right:20px;z-index:9999;' +
            'background:' + bg + ';color:white;' +
            'padding:14px 20px;border-radius:6px;' +
            'font-size:14px;font-weight:500;line-height:1.5;' +
            'max-width:420px;box-shadow:0 6px 20px rgba(0,0,0,0.15);' +
            'display:flex;align-items:center;gap:10px;' +
            'animation:toastIn 0.3s cubic-bezier(0.16,1,0.3,1);' +
            'cursor:pointer;';
        el.innerHTML = iconSvg + '<span>' + msg + '</span>';
        el.onclick = function() { el.style.opacity = '0'; el.style.transform = 'translateX(40px)'; setTimeout(function() { el.remove(); }, 200); };
        document.body.appendChild(el);
        setTimeout(function() {
            if (el.parentNode) {
                el.style.opacity = '0';
                el.style.transform = 'translateX(40px)';
                setTimeout(function() { if (el.parentNode) el.remove(); }, 300);
            }
        }, 4000);
    },

    // ---------- 自定义确认对话框 ----------
    confirm: function(msg, onYes, onNo) {
        var root = document.getElementById('modalRoot');
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.style.animation = 'none';
        overlay.innerHTML =
            '<div class="modal" style="max-width:420px;text-align:center;">' +
                '<div class="modal-body" style="padding:10px 0 20px;font-size:14px;color:var(--text);line-height:1.6;"></div>' +
                '<div class="modal-actions" style="justify-content:center;gap:12px;border:none;padding-top:0;">' +
                    '<button class="btn btn-secondary btn-cancel">取消</button>' +
                    '<button class="btn btn-gold btn-ok">确定</button>' +
                '</div>' +
            '</div>';
        overlay.querySelector('.modal-body').textContent = msg;
        overlay.querySelector('.btn-ok').onclick = function() { root.innerHTML = ''; if (onYes) onYes(); };
        overlay.querySelector('.btn-cancel').onclick = function() { root.innerHTML = ''; if (onNo) onNo(); };
        overlay.onclick = function(e) { if (e.target === overlay) { root.innerHTML = ''; if (onNo) onNo(); } };
        root.innerHTML = '';
        root.appendChild(overlay);
    },

    // ---------- 自定义输入对话框 ----------
    prompt: function(msg, defaultVal, onOk) {
        var root = document.getElementById('modalRoot');
        var overlay = document.createElement('div');
        overlay.className = 'modal-overlay';
        overlay.style.animation = 'none';
        overlay.innerHTML =
            '<div class="modal" style="max-width:420px;">' +
                '<div class="modal-body" style="padding:6px 0 10px;font-size:14px;color:var(--text);line-height:1.6;"></div>' +
                '<div class="form-group" style="margin-bottom:0;"><input type="text" id="prompt-input" style="width:100%;"></div>' +
                '<div class="modal-actions" style="justify-content:flex-end;gap:10px;border:none;padding-top:14px;">' +
                    '<button class="btn btn-secondary btn-cancel">取消</button>' +
                    '<button class="btn btn-gold btn-ok">确定</button>' +
                '</div>' +
            '</div>';
        overlay.querySelector('.modal-body').textContent = msg;
        var input = overlay.querySelector('#prompt-input');
        input.value = defaultVal || '';
        input.focus();
        input.onkeydown = function(e) { if (e.key === 'Enter') { overlay.querySelector('.btn-ok').click(); } };
        overlay.querySelector('.btn-ok').onclick = function() {
            var val = overlay.querySelector('#prompt-input').value;
            root.innerHTML = '';
            if (onOk) onOk(val);
        };
        overlay.querySelector('.btn-cancel').onclick = function() { root.innerHTML = ''; };
        overlay.onclick = function(e) { if (e.target === overlay) root.innerHTML = ''; };
        root.innerHTML = '';
        root.appendChild(overlay);
        setTimeout(function() { input.focus(); }, 100);
    }
};

// ---------- Auth & Sidebar ----------
var Auth = {
    user: null,

    requireAuth: function() {
        var self = this;
        return Api.get('/api/auth/status').then(function(status) {
            if (!status.authenticated) {
                window.location.href = '/login.html';
                return null;
            }
            self.user = status;
            self.renderSidebar();
            return status;
        }).catch(function(e) {
            window.location.href = '/login.html';
            return null;
        });
    },

    checkLogged: function() {
        return Api.get('/api/auth/status').then(function(status) {
            if (status.authenticated) {
                window.location.href = '/dashboard.html';
                return true;
            }
            return false;
        }).catch(function() { return false; });
    },

    login: function(username, password) {
        return Api.post('/api/auth/login', { username: username, password: password });
    },

    logout: function() {
        Api.post('/api/auth/logout', {}).then(function() {
            window.location.href = '/login.html';
        }).catch(function() {
            window.location.href = '/login.html';
        });
    },

    renderSidebar: function() {
        var root = document.getElementById('sidebarRoot');
        if (!root) return;
        var u = this.user;
        var isAdmin = u.role === 'ADMIN';
        var path = location.pathname;

        var sections = [
            { id: 'biz', label: '业务管理', items: [
                ['dashboard.html', '首页概览'],
                ['departments.html', '部门管理'],
                ['employees.html', '人员管理'],
                ['leave-types.html', '假别维护'],
                ['apply.html', '请假登记'],
                ['leave-list.html', '请假记录'],
                ['cancel.html', '销假管理'],
                ['balance.html', '公休假额度']
            ]},
            { id: 'report', label: '报表统计', items: [
                ['stats.html', '统计表'],
                ['monthly.html', '月度签字表'],
                ['summary.html', '汇总表']
            ]},
            { id: 'sys', label: '系统', items: [
                ['account.html', '修改密码']
            ]}
        ];
        if (isAdmin) {
            sections[2].items.unshift(['backup.html', '数据备份']);
            sections[2].items.unshift(['users.html', '用户管理']);
        }

        function buildItems(items) {
            var html = '';
            for (var i = 0; i < items.length; i++) {
                var href = items[i][0], label = items[i][1];
                var active = (path.indexOf('/' + href) >= 0) ? ' active' : '';
                html += '<a class="nav-btn' + active + '" href="/' + href + '">' + label + '</a>';
            }
            return html;
        }

        var sectionsHtml = '';
        for (var i = 0; i < sections.length; i++) {
            var sec = sections[i];
            sectionsHtml +=
                '<div class="nav-section-label" onclick="toggleNavSection(\'' + sec.id + '\')">' +
                    '<span>' + sec.label + '</span>' +
                    '<span class="collapse-icon">▼</span>' +
                '</div>' +
                '<div class="nav-section" id="nav-section-' + sec.id + '">' + buildItems(sec.items) + '</div>';
        }

        var html =
            '<aside class="sidebar">' +
                '<div class="sidebar-header">' +
                    '<div class="sidebar-logo">假</div>' +
                    '<div class="sidebar-title">请销假管理系统</div>' +
                    '<div class="sidebar-subtitle">v2.0 · 行政事业单位</div>' +
                '</div>' +
                '<div class="nav-search">' +
                    '<input type="text" id="navSearchInput" placeholder="搜索菜单…" oninput="filterNavMenu(this.value)">' +
                '</div>' +
                '<nav class="nav">' + sectionsHtml + '</nav>' +
                '<div class="sidebar-footer">' +
                    '<div id="userDisplay"><strong>' + (u.displayName || u.username) + '</strong><br>' + u.role + '</div>' +
                    '<button class="btn btn-link btn-block" id="btnLogout">退出登录</button>' +
                '</div>' +
            '</aside>';

        root.innerHTML = html;
        var btn = document.getElementById('btnLogout');
        if (btn) btn.onclick = function() { Auth.logout(); };
    },

    isViewer: function() { return this.user && this.user.role === 'VIEWER'; }
};

// ---------- 侧边栏折叠/搜索 (全局函数) ----------
function toggleNavSection(id) {
    var section = document.getElementById('nav-section-' + id);
    var label = section ? section.previousElementSibling : null;
    if (!section) return;
    if (section.style.display === 'none') {
        section.style.display = '';
        if (label) label.classList.remove('collapsed');
    } else {
        section.style.display = 'none';
        if (label) label.classList.add('collapsed');
    }
}

function filterNavMenu(keyword) {
    keyword = (keyword || '').trim().toLowerCase();
    var sections = document.querySelectorAll('.nav-section');
    var labels = document.querySelectorAll('.nav-section-label');
    if (!keyword) {
        // 恢复: 显示所有
        for (var i = 0; i < sections.length; i++) {
            sections[i].style.display = '';
            sections[i].querySelectorAll('.nav-btn').forEach(function(btn) { btn.style.display = ''; });
        }
        for (var j = 0; j < labels.length; j++) labels[j].style.display = '';
        return;
    }
    // 过滤
    for (var i = 0; i < sections.length; i++) {
        var sec = sections[i];
        var matched = false;
        var btns = sec.querySelectorAll('.nav-btn');
        for (var k = 0; k < btns.length; k++) {
            var text = btns[k].textContent.toLowerCase();
            if (text.indexOf(keyword) >= 0) {
                btns[k].style.display = '';
                matched = true;
            } else {
                btns[k].style.display = 'none';
            }
        }
        sec.style.display = matched ? '' : 'none';
        var label = sec.previousElementSibling;
        if (label) label.style.display = matched ? '' : 'none';
    }
}
