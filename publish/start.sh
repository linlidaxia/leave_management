#!/bin/bash
# ==================== 行政事业单位请销假管理系统 启动脚本 (Linux/macOS) ====================

set -e

# 切换到脚本所在目录 (确保 data.db 与 jar 同目录)
cd "$(dirname "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(pwd)"
JAR_FILE="${SCRIPT_DIR}/leave-management.jar"

# 文件不存在则报错
if [ ! -f "$JAR_FILE" ]; then
    echo "[错误] 未找到程序文件: $JAR_FILE"
    echo "请确认此 start.sh 文件与 leave-management.jar 位于同一目录。"
    exit 1
fi

# 查找 Java 17+ 运行环境
JAVA_CMD="$(command -v java || true)"
if [ -z "$JAVA_CMD" ]; then
    echo "[错误] 未检测到 Java 运行环境, 请安装 JDK 17 或更高版本。"
    echo "下载地址: https://adoptium.net/"
    exit 1
fi

# 设置默认内存参数
JVM_OPTS="-Xms256m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 给脚本添加可执行权限 (以防万一)
chmod +x "$JAR_FILE" 2>/dev/null || true

# 启动应用
echo "=================================================="
echo "  行政事业单位请销假管理系统 v2.0 启动中..."
echo "  访问地址: http://localhost:9000"
echo "  默认账号: admin / admin123"
echo "  按 Ctrl+C 可停止服务"
echo "=================================================="
echo

exec "$JAVA_CMD" $JVM_OPTS -jar "$JAR_FILE"
