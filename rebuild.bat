@echo off
chcp 65001 >nul
echo ========================================
echo   C语言自动批阅系统 - 构建脚本
echo ========================================
echo.

cd /d "%~dp0"

echo [1/2] 正在构建前端...
cd frontend
call npm run build
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] 前端构建失败！
    pause
    exit /b 1
)
cd ..

echo.
echo [2/2] 正在重启 Nginx...
docker-compose stop nginx
docker-compose rm -f nginx
docker-compose up -d nginx

echo.
echo ========================================
echo   构建完成！
echo ========================================
echo.
echo 前端地址: http://localhost
echo 后端API: http://localhost/api
echo.
pause
