-- ==================== 种子数据 ====================
-- 从原 Access 数据库 data.accdb 迁移
-- 使用 INSERT OR IGNORE 避免重复主键冲突 (每次启动都会执行)

-- ---- 兼容旧库: 给 leave_types 增加 need_attachment 列 (若不存在) ----
-- SQLite 不支持 IF NOT EXISTS 加列, 用异常捕获方式: 先查 PRAGMA, 代码层处理
-- 这里直接尝试加列, 已存在会报错但被 continue-on-error 忽略
ALTER TABLE leave_types ADD COLUMN need_attachment INTEGER DEFAULT 0;
ALTER TABLE leave_types ADD COLUMN deduct_from_annual INTEGER DEFAULT 0;

-- ---- 假别 (共 8 条, 与原 Access 数据完全一致) ----
INSERT OR IGNORE INTO leave_types (id, name, code, sort_order, remark, deduct_from_annual) VALUES
    (1, '公休假',     'GXJ',  1,   '按工龄核定：工龄<10年5天，10-20年10天，20年以上15天', 0),
    (2, '事假',       'SHIJ', 2,   '事假可抵扣公休假', 1),
    (3, '病假',       'BINJ', 3,   '需提供医院证明', 0),
    (4, '婚丧假',     'HSJ',  4,   '婚假25天，丧假3天（直系亲属）', 0),
    (5, '男方陪护假', 'PHJ',  5,   '男方护理假，一般15天', 0),
    (6, '调休',       'TXJ',  6,   '调休假', 0),
    (7, '其他',       'QIT',  99,  '其他假别', 0),
    (8, '产假',       'CJ',   999, '需提供医院证明', 0);

-- 自增主键续接 (避免后续 INSERT 与固定 ID 冲突)
DELETE FROM sqlite_sequence WHERE name='leave_types';
INSERT INTO sqlite_sequence (name, seq) SELECT 'leave_types', COALESCE(MAX(id), 0) FROM leave_types;

-- ---- 系统设置 (与原 Access 数据一致) ----
INSERT OR IGNORE INTO sys_settings (id, key_name, value_text) VALUES
    (1, 'app_name',  '行政事业单位请销假管理系统'),
    (2, 'version',   '2.0.0'),
    (3, 'init_year', '2026');

-- ---- 默认用户账号 (Web 版新增) ----
-- 密码使用 BCrypt 加密:
--   admin/admin123     -> ROLE_ADMIN  (全权 + 用户管理 + 备份恢复)
--   operator/operator123 -> ROLE_USER  (日常操作)
--   viewer/viewer123   -> ROLE_VIEWER (只读)
INSERT OR IGNORE INTO users (id, username, password_hash, display_name, role, enabled) VALUES
    (1, 'admin',    '$2b$10$8PdWQRl0ToUoViMMKBIJie.YC8.4a9wtxyD0R2N9q8txwoLdHBrki', '系统管理员', 'ADMIN',  1),
    (2, 'operator', '$2b$10$x.QfTsXXwdwickjt8m0X5.bYFz.RLrduFwbyrOpwY6iEQQTAvpiiG', '操作员',     'USER',   1),
    (3, 'viewer',   '$2b$10$KZ1S3Brdt/x4xhEjcNFV1Ow4ziM7Jt2phe5id80AIKT2PQWVlX80.', '查看员',     'VIEWER', 1);

DELETE FROM sqlite_sequence WHERE name='users';
INSERT INTO sqlite_sequence (name, seq) SELECT 'users', COALESCE(MAX(id), 0) FROM users;
