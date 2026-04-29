@echo off
chcp 936 >nul
echo ========================================
echo   C语言自动批阅系统 - 关闭脚本
echo ========================================
echo.

cd /d "%~dp0"

echo 正在停止所有 Docker 服务...
docker-compose down

echo.
echo ========================================
echo   所有服务已停止！
echo ========================================
echo.
echo 如需重新启动: start.bat
echo.
pause
