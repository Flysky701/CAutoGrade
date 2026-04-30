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

echo [1/4] 安装前端依赖...
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

echo.
echo [2/4] 构建前端...
call npm run build
if %errorlevel% neq 0 (
    echo [错误] 前端构建失败
    pause
    exit /b 1
)
cd ..

echo.
echo [3/4] 构建后端...
cd backend
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] 后端构建失败
    pause
    exit /b 1
)
cd ..

echo.
echo [4/4] 重新部署容器...
echo   → 清除Docker构建缓存并重建...
docker-compose build --no-cache backend grading-worker
if %errorlevel% neq 0 (
    echo [错误] Docker构建失败
    pause
    exit /b 1
)
echo   → 启动所有服务...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [错误] 容器启动失败
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
echo 提示：首次访问请 Ctrl+Shift+R 强制刷新浏览器缓存
echo.
pause
endlocal
