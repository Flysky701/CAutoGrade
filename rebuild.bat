@echo off
chcp 936 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   C AutoGrade - Rebuild Script
echo ========================================
echo.

where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Please install it first.
    pause
    exit /b 1
)

echo [1/4] Installing frontend dependencies...
cd frontend
if not exist "node_modules" (
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] Dependency installation failed
        pause
        exit /b 1
    )
) else (
    echo Dependencies already installed
)

echo.
echo [2/4] Building frontend...
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] Frontend build failed
    pause
    exit /b 1
)
cd ..

echo.
echo [3/4] Building backend...
cd backend
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Backend build failed
    pause
    exit /b 1
)
cd ..

echo.
echo [4/4] Rebuilding Docker images...
echo   Rebuilding backend image (no cache)...
docker compose build --no-cache backend
docker compose build grading-worker
if %errorlevel% neq 0 (
    echo [ERROR] Docker build failed
    pause
    exit /b 1
)

echo   Starting all services...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start services
    pause
    exit /b 1
)
echo.
echo ========================================
echo   Rebuild complete!
echo ========================================
echo.
echo Frontend:  http://localhost
echo API Docs:  http://localhost/doc.html
echo.
echo Tip: On first visit, press Ctrl+Shift+R to force refresh browser cache.
echo.
pause
endlocal
