@echo off
chcp 65001 >nul
echo ============================================
echo   论文生成系统 - 启动脚本
echo ============================================
echo.

:: 设置 Minecraft 自带的 JDK 21
set JAVA_HOME=%APPDATA%\.minecraft\runtime\java-runtime-delta
set PATH=%JAVA_HOME%\bin;%PATH%

echo   JAVA_HOME = %APPDATA%\.minecraft\runtime\java-runtime-delta
echo   PostgreSQL: localhost:5432 (需服务已启动)
echo   Redis: 需手动启动 redis-server
echo.
echo   启动后访问: http://localhost:8080/doc.html
echo ============================================

:: 启动 Redis（如果未运行）
start /B "" "C:\Program Files\Redis\redis-server.exe" 2>nul

:: 启动 Spring Boot
mvnw.cmd spring-boot:run

pause
