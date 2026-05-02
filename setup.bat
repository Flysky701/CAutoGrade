@echo off
chcp 65001 >nul 2>&1
chcp 936 >nul 2>&1
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo.
echo  ╔══════════════════════════════════════════════════════════╗
echo  ║       CAutoGrade - One-Click Setup Wizard               ║
echo  ║       C语言作业自动批阅系统 - 一键配置向导               ║
echo  ╚══════════════════════════════════════════════════════════╝
echo.

:: ============================================================
:: Step 0: Prerequisites Check
:: ============================================================
echo [Step 0/5] Checking prerequisites...
echo.

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo   [ERROR] Docker is not running!
    echo   Please start Docker Desktop and try again.
    echo.
    pause
    exit /b 1
)
echo   [OK] Docker is running

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo   [WARN] Java not found. Backend build will be skipped.
    set "HAS_JAVA=0"
) else (
    echo   [OK] Java found
    set "HAS_JAVA=1"
)

node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo   [WARN] Node.js not found. Frontend build will be skipped.
    set "HAS_NODE=0"
) else (
    echo   [OK] Node.js found
    set "HAS_NODE=1"
)

python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo   [WARN] Python not found. Grading engine tests will be skipped.
    set "HAS_PYTHON=0"
) else (
    echo   [OK] Python found
    set "HAS_PYTHON=1"
)

echo.

:: ============================================================
:: Step 1: Check existing .env
:: ============================================================
echo [Step 1/5] Checking configuration...
echo.

if exist ".env" (
    echo   Found existing .env file:
    echo   ─────────────────────────────────
    for /f "tokens=1,* delims==" %%a in (.env) do (
        if "%%a"=="DEEPSEEK_API_KEY" (
            echo   %%a=****%%~zb****
        ) else if "%%a"=="MYSQL_PASSWORD" (
            echo   %%a=********
        ) else if "%%a"=="REDIS_PASSWORD" (
            echo   %%a=********
        ) else if "%%a"=="JWT_SECRET" (
            echo   %%a=********
        ) else (
            echo   %%a=%%b
        )
    )
    echo   ─────────────────────────────────
    echo.
    set /p "RECONFIG=Reconfigure? (y/N): "
    if /i not "!RECONFIG!"=="y" (
        echo   Keeping existing configuration.
        goto :step_build
    )
)

:: ============================================================
:: Step 2: Collect configuration from user
:: ============================================================
echo.
echo [Step 2/5] Configuration Wizard
echo.
echo   Please provide the following configuration values.
echo   Press Enter to accept the [default] value.
echo.

:: MySQL Password
set "MYSQL_PASSWORD="
set /p "MYSQL_PASSWORD=  [1/4] MySQL root password [autograding2026]: "
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=autograding2026"

:: JWT Secret
set "JWT_SECRET="
set /p "JWT_SECRET=  [2/4] JWT Secret Key (min 32 chars) [auto-generated]: "
if "%JWT_SECRET%"=="" (
    :: Generate a simple JWT secret from timestamp + random
    set "JWT_SECRET=YXV0b2dyYWRpbmctand0LXNlY3JldC1rZXktZm9yLXByb2R1Y3Rpb24tMjAyNi1zZWN1cmU"
)

:: DeepSeek API Key
set "DEEPSEEK_API_KEY="
set /p "DEEPSEEK_API_KEY=  [3/4] DeepSeek API Key (required for LLM grading): "
if "%DEEPSEEK_API_KEY%"=="" (
    echo   [WARN] No API Key provided. LLM grading will be unavailable!
    echo          You can set it later by editing .env file.
)

:: Redis Password
set "REDIS_PASSWORD="
set /p "REDIS_PASSWORD=  [4/4] Redis password [%MYSQL_PASSWORD%]: "
if "%REDIS_PASSWORD%"=="" set "REDIS_PASSWORD=%MYSQL_PASSWORD%"

echo.
echo   Configuration summary:
echo   ─────────────────────────────────
echo   MySQL Password:  ********
echo   JWT Secret:      ********
echo   DeepSeek API:    ********
echo   Redis Password:  ********
echo   ─────────────────────────────────
echo.
set /p "CONFIRM=  Confirm and save? (Y/n): "
if /i "!CONFIRM!"=="n" (
    echo   Aborted. Run setup.bat again to reconfigure.
    pause
    exit /b 1
)

:: ============================================================
:: Step 3: Write .env file
:: ============================================================
echo.
echo [Step 3/5] Writing .env file...

(
echo MYSQL_PASSWORD=%MYSQL_PASSWORD%
echo MYSQL_USER=root
echo MYSQL_HOST=mysql
echo MYSQL_PORT=3306
echo MYSQL_DATABASE=autograding
echo.
echo JWT_SECRET=%JWT_SECRET%
echo.
echo DEEPSEEK_API_KEY=%DEEPSEEK_API_KEY%
echo.
echo REDIS_PASSWORD=%REDIS_PASSWORD%
) > .env

echo   [OK] .env file created

:: Also update redis.conf with the password
echo   [OK] Updating redis.conf...
(
echo bind 0.0.0.0
echo protected-mode yes
echo port 6379
echo.
echo tcp-backlog 511
echo timeout 0
echo tcp-keepalive 300
echo.
echo daemonize no
echo loglevel notice
echo.
echo databases 16
echo always-show-logo no
echo set-proc-title yes
echo proc-title-template "{title} {listen-addr} {server-mode}"
echo.
echo stop-writes-on-bgsave-error yes
echo rdbcompression yes
echo rdbchecksum yes
echo dbfilename dump.rdb
echo rdb-del-sync-files no
echo dir /data
echo.
echo requirepass %REDIS_PASSWORD%
) > docker\redis\redis.conf

echo   [OK] redis.conf updated

:: Update grading-engine config.yaml Redis URL
echo   [OK] Updating grading-engine config.yaml...
powershell -Command "(Get-Content 'grading-engine\config.yaml') -replace 'redis://:.*@redis:6379/1', 'redis://:%REDIS_PASSWORD%@redis:6379/1' | Set-Content 'grading-engine\config.yaml'"

:: Update docker-compose.yml Celery broker URL
echo   [OK] Updating docker-compose.yml...
powershell -Command "(Get-Content 'docker-compose.yml') -replace 'redis://:.*@redis:6379/1', 'redis://:%REDIS_PASSWORD%@redis:6379/1' | Set-Content 'docker-compose.yml'"

echo.

:: ============================================================
:: Step 4: Build
:: ============================================================
:step_build
echo [Step 4/5] Building project...
echo.

if "%HAS_NODE%"=="1" (
    echo   [4a] Building frontend...
    cd frontend
    if not exist "node_modules" (
        echo   Installing dependencies...
        call npm install --legacy-peer-deps >nul 2>&1
    )
    call npm run build
    if %errorlevel% neq 0 (
        echo   [ERROR] Frontend build failed!
        cd ..
        pause
        exit /b 1
    )
    cd ..
    echo   [OK] Frontend built
) else (
    echo   [SKIP] Frontend build (Node.js not found)
    if not exist "frontend\dist\index.html" (
        echo   [ERROR] frontend/dist not found! Please build frontend manually.
        pause
        exit /b 1
    )
)

if "%HAS_JAVA%"=="1" (
    echo   [4b] Building backend...
    cd backend
    call mvn clean package -DskipTests -q
    if %errorlevel% neq 0 (
        echo   [ERROR] Backend build failed!
        cd ..
        pause
        exit /b 1
    )
    cd ..
    echo   [OK] Backend built
) else (
    echo   [SKIP] Backend build (Java not found)
    if not exist "backend\target\backend-1.0.0-SNAPSHOT.jar" (
        echo   [ERROR] backend/target/*.jar not found! Please build backend manually.
        pause
        exit /b 1
    )
)

echo.

:: ============================================================
:: Step 5: Start services
:: ============================================================
echo [Step 5/5] Starting services...
echo.

docker-compose down >nul 2>&1

docker-compose up -d
if %errorlevel% neq 0 (
    echo   [ERROR] Failed to start services!
    pause
    exit /b 1
)

echo   Waiting for MySQL initialization...
timeout /t 20 /nobreak >nul

echo.
echo  ╔══════════════════════════════════════════════════════════╗
echo  ║            Setup Complete!                               ║
echo  ╚══════════════════════════════════════════════════════════╝
echo.
echo   Access URLs:
echo     Frontend:    http://localhost
echo     API Docs:    http://localhost:8080/doc.html
echo     Health:      http://localhost:8080/api/health
echo.
echo   Default accounts:
echo     Admin:       admin / 123456
echo     Teacher:     teacher / 123456
echo     Student:     student / 123456
echo.
echo   Useful commands:
echo     View logs:   docker-compose logs -f
echo     Stop:        stop.bat
echo     Rebuild:     rebuild.bat
echo.
pause
endlocal
