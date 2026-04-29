@echo off
chcp 936 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   C语言自动批阅系统 - 构建脚本
echo ========================================
echo.

REM 检查 Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Node.js，请先安装
    pause
    exit /b 1
)

echo [1/3] 安装前端依赖...
cd frontend
if not exist "node_modules" (
    call npm install
    if %errorlevel% neq 0 (
        echo [错误] 依赖安装失败
        pause
        exit /b 1
    )
) else (
    echo 已安装，跳过
)
cd ..

echo.
echo [2/3] 构建后端...
cd backend
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] 后端构建失败
    pause
    exit /b 1
)
cd ..

echo.
echo [3/3] 重新部署容器...
docker-compose up -d --build backend grading-worker
if %errorlevel% neq 0 (
    echo [错误] 容器部署失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo   构建完成！
echo ========================================
echo.
echo 前端地址:  http://localhost
echo API文档:   http://localhost/doc.html
echo.
pause
endlocal
