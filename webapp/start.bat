@echo off
REM ==========================================================
REM Leave Management System - Windows Startup Script
REM ==========================================================

REM Switch to this script's directory (so data.db is next to jar)
cd /d "%~dp0"
set "APP_DIR=%CD%"
set "JAR_FILE=%APP_DIR%\leave-management.jar"

REM Check jar exists
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR file not found: %JAR_FILE%
    echo Please make sure start.bat and leave-management.jar are in the same folder.
    pause
    exit /b 1
)

REM Check Java is available
set "JAVA_CMD=java"
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java runtime not found. Please install JDK 17 or later.
    echo Download: https://adoptium.net/
    pause
    exit /b 1
)

REM JVM options
set "JVM_OPTS=-Xms256m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -Dsun.jnu.encoding=UTF-8"

REM Switch console to UTF-8 (for Chinese output below)
chcp 65001 >nul

REM Start the application (foreground)
echo ==================================================
echo   Leave Management System v2.0 starting...
echo   URL:   http://localhost:9000
echo   Login: admin / admin123
echo   Press Ctrl+C to stop the service
echo ==================================================
echo.

%JAVA_CMD% %JVM_OPTS% -jar "%JAR_FILE%"
