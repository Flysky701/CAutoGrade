@echo off
chcp 65001 >nul
echo ==============================================
echo         C语言自动批阅系统 - 启动脚本
echo ==============================================
echo.

set "WORK_DIR=d:\WorkSpace\Management_Sys"
set "FRONTEND_DIR=%WORK_DIR%\frontend"

echo [1/4] 检查 Docker 是否运行...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker Desktop 未启动！请先启动 Docker Desktop
    pause
    exit /b 1
)
echo OK - Docker 已运行

echo.
echo [2/4] 启动数据库和中间件...
cd /d %WORK_DIR%
docker-compose up -d mysql redis chromadb
echo 等待数据库启动...
timeout /t 10 /nobreak >nul
echo OK - 数据库已启动

echo.
echo [3/4] 启动后端服务...
docker-compose up -d backend
echo 等待后端启动...
timeout /t 15 /nobreak >nul
echo OK - 后端已启动

echo.
echo [4/4] 启动前端开发服务器...
cd /d %FRONTEND_DIR%
echo 正在启动前端开发服务器...
start cmd /k "npm run dev"

echo.
echo ==============================================
echo          服务启动完成！
echo ==============================================
echo 前端页面: http://localhost:3000
echo 后端API:  http://localhost:8080
echo API文档:  http://localhost:8080/doc.html
echo ==============================================
echo 初始账号: admin / admin123 (可能需要重新注册)
echo          teacher1 / admin123
echo          student1 / admin123
echo ==============================================
echo 注意: 如果初始账号登录失败，请先注册新账号
echo ==============================================
echo.
pause