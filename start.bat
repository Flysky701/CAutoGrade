@echo off
chcp 936 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   C AutoGrade - Start Services
echo ========================================
echo.

if not exist ".env" (
    echo [ERROR] .env file not found!
    echo   Please run setup.bat first to configure the system.
    echo.
    pause
    exit /b 1
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop.
    pause
    exit /b 1
)

echo [1/2] Starting services...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start services. Check docker-compose.yml
    pause
    exit /b 1
)

echo [2/2] Waiting for services...
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo   Services started!
echo ========================================
echo.
echo   Frontend:  http://localhost
echo   API Docs:  http://localhost:8080/doc.html
echo.
echo   Accounts:  admin/teacher/student, password: 123456
echo.
echo   Commands:  stop.bat | rebuild.bat | docker-compose logs -f
echo.
pause
endlocal
