@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   C语言自动批阅系统 - 启动脚本
echo ========================================
echo.

REM 检查 Docker 是否运行
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop
    pause
    exit /b 1
)

echo [1/2] 正在启动服务容器...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [错误] 容器启动失败，请检查 docker-compose.yml
    pause
    exit /b 1
)

echo [2/2] 等待服务就绪...
timeout /t 8 /nobreak >nul

echo.
echo ========================================
echo   启动完成！
echo ========================================
echo.
echo 访问地址:
echo   前端:   http://localhost
echo   API文档: http://localhost/doc.html
echo.
echo 初始账号:
echo   管理员: admin / admin123
echo   教师:   teacher / 123456
echo   学生:   student / 123456
echo.
echo 常用命令:
echo   查看日志: docker-compose logs -f
echo   停止服务: docker-compose down
echo.
pause
endlocal
