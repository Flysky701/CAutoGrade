@echo off
chcp 936 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   C AutoGrade - Start Script
echo ========================================
echo.

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop.
    pause
    exit /b 1
)

echo [1/3] Checking environment variables...
if "%MYSQL_PASSWORD%"=="" (
    echo [WARN] MYSQL_PASSWORD not set. Using default.
)
if "%JWT_SECRET%"=="" (
    echo [WARN] JWT_SECRET not set. Please set it before deployment.
)
if "%DEEPSEEK_API_KEY%"=="" (
    echo [WARN] DEEPSEEK_API_KEY not set. LLM grading will be unavailable.
)

echo [2/3] Starting services...
docker-compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start services. Check docker-compose.yml
    pause
    exit /b 1
)

echo [3/3] Waiting for services...
timeout /t 8 /nobreak >nul

echo.
echo ========================================
echo   Services started!
echo ========================================
echo.
echo Access URLs:
echo   Frontend:  http://localhost
echo   API Docs:  http://localhost/doc.html
echo.
echo Default accounts (please change passwords after first login):
echo   See deployment documentation for initial credentials.
echo.
echo Useful commands:
echo   View logs:  docker-compose logs -f
echo   Stop:       docker-compose down
echo.
pause
endlocal
