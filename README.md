# 行政事业单位请销假管理系统 v2.0 (Web Edition)

> 基于 Spring Boot 3 + SQLite 的轻量级请销假管理系统，单文件部署，开箱即用。

---

## 设计思路

### 为什么做这个系统

原系统为 Python + customtkinter + Microsoft Access 桌面应用，仅支持单机 Windows，无法多人协作，Access 数据库易损坏。Web 版解决以下痛点：

| 旧版问题 | Web 版方案 |
|---------|----------|
| 单机使用，无法多人同时操作 | 浏览器访问，支持多用户并发 |
| Access 数据库易损坏 | SQLite 单文件，稳定可靠 |
| Windows 专属 | 跨平台（Windows/Linux/macOS） |
| 需安装 Python + 依赖 | 单个 jar 文件，有 Java 即可运行 |
| 无审计日志 | 每次操作带登录用户记录 |

### 核心设计原则

1. **极简部署**：整个系统打包为一个 56MB 的 fat-jar，配合 `start.bat`/`start.sh` 一键启动
2. **零外部依赖**：内嵌 SQLite，无需安装数据库；内嵌 Tomcat，无需 Web 服务器
3. **业务完整性**：完整复刻原 Python 版全部业务规则，包括公休假额度控制、事假自动抵扣、销假登记等
4. **响应式设计**：支持 PC、平板、手机浏览器访问

---

## 技术栈

| 层次 | 技术 | 说明 |
|-----|------|------|
| 后端框架 | Spring Boot 3.3.4 | 自动配置、内嵌 Tomcat |
| 数据库 | SQLite 3 (via sqlite-jdbc) | 单文件，零配置 |
| ORM | Spring JdbcTemplate | 轻量级，无需 MyBatis/JPA |
| 安全 | Spring Security (Session) | 登录认证 + 角色授权 |
| Excel | Apache POI | 导入导出 .xlsx |
| 前端 | 原生 HTML/CSS/JS | 无框架依赖，ES5 兼容 |
| 构建 | Maven | 标准化构建流程 |

---

## 功能模块

### 业务管理

- **部门管理**：增删改查 + Excel 批量导入
- **人员管理**：增删改查 + 工龄自动计算 + 年假余额展示 + Excel 批量导入
- **假别管理**：8 种预置假别 + 自定义假别 + 附件必需标记
- **请假登记**：表单填写 + 天数自动计算 + 事假抵扣公休假预览 + 附件上传
- **请假记录**：多条件查询 + 分页 + Excel 导出 + 审批 + 编辑 + 删除 + 附件管理
- **销假管理**：待销假列表 + 多条件筛选 + 销假登记 + 已销假记录

### 报表统计

- **公休假额度表**：按年度/部门查询 + 初始化额度 + Excel 导出
- **请假统计表**：按部门/假别统计人次天数 + Excel 导出
- **月度签字表**：按月份查看请假明细 + Excel 导出
- **请假汇总表**：各部门各类假汇总 + Excel 导出

### 系统管理

- **用户管理**：三级角色（ADMIN/USER/VIEWER）+ 密码修改
- **数据备份**：一键备份 SQLite 数据库文件
- **数据还原**：上传备份文件恢复数据

### 业务规则

| 规则 | 说明 |
|------|------|
| 工龄计算 | 从 `work_start_date` 到当前日期，按满年计算 |
| 公休假额度 | 工龄 <10 年 = 5 天，10-20 年 = 10 天，≥20 年 = 15 天 |
| 请假天数 | 支持半天（0.5 天），上午/下午时段 |
| 事假抵扣 | 事假自动抵扣公休假，抵扣额不超过余额 |
| 年假超额控制 | 公休假申请超过剩余额度时阻止提交 |
| 销假恢复 | 删除请假记录时自动恢复已抵扣/已使用的年假天数 |

---

## 项目结构

```
leave_management/
├── webapp/                          # Spring Boot 项目
│   ├── pom.xml                      # Maven 配置
│   ├── start.bat                    # Windows 启动脚本
│   ├── start.sh                     # Linux/macOS 启动脚本
│   └── src/main/
│       ├── java/com/leavemgmt/
│       │   ├── LeaveManagementApplication.java   # 启动类
│       │   ├── config/
│       │   │   ├── SecurityConfig.java           # Spring Security 配置
│       │   │   └── JdbcUserDetailsService.java   # 用户认证服务
│       │   ├── controller/
│       │   │   ├── AuthController.java           # 登录/登出/改密
│       │   │   ├── LeaveController.java          # 全部业务 API
│       │   │   ├── AdminUserController.java      # 用户管理 API
│       │   │   └── BackupController.java         # 备份还原 API
│       │   ├── service/
│       │   │   ├── LeaveService.java             # 核心业务逻辑
│       │   │   ├── UserService.java              # 用户 CRUD
│       │   │   ├── BackupService.java            # 数据库备份
│       │   │   ├── ExcelExportService.java       # Excel 导出
│       │   │   └── DataImportService.java        # Excel 导入
│       │   ├── repository/                       # 数据访问层 (JdbcTemplate)
│       │   ├── model/                            # 数据模型
│       │   └── util/
│       │       ├── LeaveCalculator.java          # 请假天数/工龄计算
│       │       └── SqliteDateUtil.java           # SQLite 日期转换
│       └── resources/
│           ├── application.properties            # 应用配置
│           ├── schema.sql                        # 建表语句
│           ├── data.sql                          # 初始数据
│           └── static/                           # 前端静态资源
│               ├── login.html                    # 登录页
│               ├── index.html                    # 首页
│               ├── css/app.css                   # 全局样式
│               └── js/
│                   ├── common.js                 # 共享工具 (API/UI/Auth)
│                   └── pages/*.js                # 各页面 JS
└── publish/                         # 最终交付目录
    ├── leave-management.jar         # 可执行 jar (需自行构建)
    ├── start.bat                    # Windows 启动脚本
    └── start.sh                     # Linux/macOS 启动脚本
```

---

## 数据库设计

SQLite 数据库文件 `data.db` 位于 jar 同目录，启动时自动创建。

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `departments` | 部门 | id, name, code |
| `employees` | 员工 | id, name, department_id, work_start_date |
| `leave_types` | 假别 | id, name, need_attachment |
| `leave_applications` | 请假申请 | id, employee_id, leave_type_id, status, offset_annual |
| `leave_cancellations` | 销假记录 | id, application_id, cancel_date, actual_days |
| `annual_leave_balance` | 年假额度 | id, employee_id, year_val, total_days, used_days |
| `leave_attachments` | 附件 | id, application_id, file_data (BLOB) |
| `users` | 用户账号 | id, username, password_hash (BCrypt), role |
| `sys_settings` | 系统设置 | key_name, value_text |

---

## 部署步骤

### 环境要求

- **Java 17+**（推荐 Java 21）
- 无需安装数据库、Web 服务器或任何中间件

### 方式一：直接运行发布包

1. 从 `publish/` 目录获取三个文件：
   - `leave-management.jar`
   - `start.bat`（Windows）
   - `start.sh`（Linux/macOS）
2. 将三个文件放在同一目录
3. 双击 `start.bat`（Windows）或执行 `bash start.sh`（Linux/macOS）
4. 浏览器访问 `http://localhost:9000`

### 方式二：从源码构建

```bash
# 1. 克隆仓库
git clone git@github.com:linlidaxia/leave_management.git
cd leave_management/webapp

# 2. 确保 JAVA_HOME 指向 JDK 17+
export JAVA_HOME=/path/to/jdk-17
# Windows PowerShell:
# $env:JAVA_HOME = "D:\java\jdk-21.0.3.9-hotspot"

# 3. 构建
mvn -B clean package -DskipTests

# 4. 运行
java -jar target/leave-management.jar

# 5. 或复制到发布目录运行
cp target/leave-management.jar ../publish/
cd ../publish
java -jar leave-management.jar
```

### 修改端口

编辑 `application.properties`：

```properties
server.port=9000
```

或启动时指定：

```bash
java -jar leave-management.jar --server.port=8080
```

---

## 默认账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | `admin123` | ADMIN | 系统管理员，全权操作 |
| `operator` | `operator123` | USER | 操作员，日常业务操作 |
| `viewer` | `viewer123` | VIEWER | 查看员，只读访问 |

> 首次登录后请修改默认密码。

---

## 角色权限

| 功能 | ADMIN | USER | VIEWER |
|------|:-----:|:----:|:------:|
| 查看数据 | ✓ | ✓ | ✓ |
| 请假登记/编辑/删除 | ✓ | ✓ | ✗ |
| 审批/销假 | ✓ | ✓ | ✗ |
| 附件上传/删除 | ✓ | ✓ | ✗ |
| 部门/人员/假别管理 | ✓ | ✓ | ✗ |
| Excel 导入导出 | ✓ | ✓ | ✗ |
| 用户管理 | ✓ | ✗ | ✗ |
| 数据备份/还原 | ✓ | ✗ | ✗ |

---

## API 接口

所有 API 以 `/api` 为前缀，需要登录（Session 认证）。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/status` | 当前用户状态 |
| GET/POST | `/api/departments` | 部门列表/新建 |
| GET/POST | `/api/employees` | 员工列表/新建 |
| GET | `/api/leave-types` | 假别列表 |
| GET/POST | `/api/applications` | 请假记录列表/新建 |
| POST | `/api/applications/{id}/approve` | 审批 |
| POST | `/api/cancellations` | 销假登记 |
| GET | `/api/cancellations/pending` | 待销假列表 |
| GET | `/api/annual-balances` | 年假额度列表 |
| POST | `/api/annual-balances/init` | 初始化年假额度 |
| GET | `/api/export/*` | Excel 导出 |
| POST | `/api/import/*` | Excel 导入 |

---

## 常见问题

### Q: 启动报错 "端口已被占用"

```bash
# 查找占用 9000 端口的进程
netstat -ano | findstr :9000
# 终止该进程
taskkill /PID <进程ID> /F
```

### Q: 如何修改默认密码

登录后点击右上角用户菜单 → 修改密码。

### Q: 数据库文件在哪里

`data.db` 位于 jar 文件同一目录，备份即复制此文件。

### Q: 如何迁移数据

复制 `data.db` 文件到新服务器同目录即可。

---

## 开发说明

### 构建环境

- JDK 17+（推荐 21）
- Maven 3.6+
- 无 IDE 要求（IntelliJ / VS Code / Eclipse 均可）

### 代码规范

- 后端：标准 Spring Boot 分层架构（Controller → Service → Repository）
- 前端：纯 ES5 JavaScript，无框架依赖，无 npm
- 样式：扁平化设计，深墨色 (#1A1F2E) + 暖纸色 (#F7F4ED) + 金色 (#B8945F) 配色
- 数据库：SQLite，日期字段存储为 TEXT，使用 `SqliteDateUtil` 转换

### 运行测试

```bash
cd webapp
mvn test
```

---

## 许可证

MIT License
