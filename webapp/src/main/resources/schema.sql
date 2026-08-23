-- ==================== SQLite Schema ====================
-- 行政事业单位请销假管理系统 v2.0 (Web Edition)

-- 人员身份表
CREATE TABLE IF NOT EXISTS employee_identities (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    code        TEXT,
    sort_order  INTEGER DEFAULT 0,
    remark      TEXT,
    created_at  TEXT DEFAULT (datetime('now','localtime'))
);

-- 部门表
CREATE TABLE IF NOT EXISTS departments (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    code        TEXT,
    sort_order  INTEGER DEFAULT 0,
    remark      TEXT,
    created_at  TEXT DEFAULT (datetime('now','localtime'))
);

-- 员工表
CREATE TABLE IF NOT EXISTS employees (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT NOT NULL,
    gender           TEXT,
    id_card          TEXT,
    department_id    INTEGER,
    identity_id      INTEGER,
    position         TEXT,
    work_start_date  TEXT,
    phone            TEXT,
    remark           TEXT,
    created_at       TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (department_id) REFERENCES departments(id),
    FOREIGN KEY (identity_id) REFERENCES employee_identities(id)
);
CREATE INDEX IF NOT EXISTS idx_employees_dept ON employees(department_id);
CREATE INDEX IF NOT EXISTS idx_employees_identity ON employees(identity_id);

-- 假别表
CREATE TABLE IF NOT EXISTS leave_types (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL,
    code            TEXT NOT NULL,
    sort_order       INTEGER DEFAULT 0,
    need_attachment  INTEGER DEFAULT 0,   -- 0: 不需要佐证, 1: 需要佐证
    remark          TEXT
);

-- 请假申请表
-- status: 待审批 / 已审批 / 已销假
CREATE TABLE IF NOT EXISTS leave_applications (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id     INTEGER NOT NULL,
    leave_type_id   INTEGER NOT NULL,
    start_date      TEXT NOT NULL,
    start_period    TEXT NOT NULL,   -- 上午 / 下午
    end_date        TEXT NOT NULL,
    end_period      TEXT NOT NULL,
    days            REAL NOT NULL,
    reason          TEXT,
    status          TEXT DEFAULT '待审批',
    apply_date      TEXT DEFAULT (datetime('now','localtime')),
    approver        TEXT,
    approve_date    TEXT,
    offset_annual   REAL DEFAULT 0,  -- 事假抵扣公休假天数
    remark          TEXT,
    created_at      TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (employee_id)   REFERENCES employees(id),
    FOREIGN KEY (leave_type_id) REFERENCES leave_types(id)
);
CREATE INDEX IF NOT EXISTS idx_apps_emp        ON leave_applications(employee_id);
CREATE INDEX IF NOT EXISTS idx_apps_start_date ON leave_applications(start_date);
CREATE INDEX IF NOT EXISTS idx_apps_status     ON leave_applications(status);

-- 销假记录表
CREATE TABLE IF NOT EXISTS leave_cancellations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id  INTEGER NOT NULL,
    cancel_date     TEXT NOT NULL,
    actual_days     REAL NOT NULL,
    remark          TEXT,
    created_at      TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (application_id) REFERENCES leave_applications(id)
);
CREATE INDEX IF NOT EXISTS idx_cancel_app ON leave_cancellations(application_id);

-- 请假佐证附件表
CREATE TABLE IF NOT EXISTS leave_attachments (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    application_id  INTEGER NOT NULL,
    file_name       TEXT NOT NULL,        -- 原始文件名
    file_size       INTEGER NOT NULL,     -- 字节数
    content_type    TEXT NOT NULL,        -- MIME 类型
    file_data       BLOB NOT NULL,        -- 二进制内容
    uploaded_at     TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (application_id) REFERENCES leave_applications(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_attach_app ON leave_attachments(application_id);

-- 公休假额度表
CREATE TABLE IF NOT EXISTS annual_leave_balance (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id     INTEGER NOT NULL,
    year_val        INTEGER NOT NULL,
    work_years      INTEGER,
    total_days      REAL,
    used_days       REAL DEFAULT 0,
    remaining_days  REAL,
    remark          TEXT,
    UNIQUE (employee_id, year_val),
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);
CREATE INDEX IF NOT EXISTS idx_balance_emp_year ON annual_leave_balance(employee_id, year_val);

-- 系统设置表
CREATE TABLE IF NOT EXISTS sys_settings (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    key_name    TEXT NOT NULL UNIQUE,
    value_text  TEXT
);

-- ==================== 用户与权限表 (Web 新增) ====================
-- role: ADMIN / USER / VIEWER
CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    display_name    TEXT,
    role            TEXT NOT NULL DEFAULT 'USER',
    enabled         INTEGER DEFAULT 1,
    created_at      TEXT DEFAULT (datetime('now','localtime'))
);
