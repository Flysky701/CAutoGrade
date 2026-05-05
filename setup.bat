@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo.
echo  +============================================================+
echo  ^|       CAutoGrade - One-Click Setup Wizard                ^|
echo  ^|       C语言作业自动批阅系统 - 一键配置向导                 ^|
echo  +============================================================+
echo.

:: ============================================================
:: Step 0: Prerequisites Check
:: ============================================================
echo [Step 0/6] Checking prerequisites...
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
echo [Step 1/6] Checking configuration...
echo.

if exist ".env" (
    echo   Found existing .env file:
    echo   ------------------------------------
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if /i "%%a"=="DEEPSEEK_API_KEY" (
            echo   %%a=****
        ) else if /i "%%a"=="MYSQL_PASSWORD" (
            echo   %%a=********
        ) else if /i "%%a"=="REDIS_PASSWORD" (
            echo   %%a=********
        ) else if /i "%%a"=="JWT_SECRET" (
            echo   %%a=********
        ) else (
            echo   %%a=%%b
        )
    )
    echo   ------------------------------------
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
echo [Step 2/6] Configuration Wizard
echo.
echo   Please provide the following configuration values.
echo   Press Enter to accept the [default] value.
echo.

:: MySQL Password
set "MYSQL_PASSWORD="
set /p "MYSQL_PASSWORD=  [1/9] MySQL root password [autograding2026]: "
if "%MYSQL_PASSWORD%"=="" set "MYSQL_PASSWORD=autograding2026"

:: JWT Secret
set "JWT_SECRET="
set /p "JWT_SECRET=  [2/9] JWT Secret Key (min 32 chars) [auto-generated]: "
if "%JWT_SECRET%"=="" (
    set "JWT_SECRET=YXV0b2dyYWRpbmctand0LXNlY3JldC1rZXktZm9yLXByb2R1Y3Rpb24tMjAyNi1zZWN1cmU"
)

:: DeepSeek API Key
set "DEEPSEEK_API_KEY="
set /p "DEEPSEEK_API_KEY=  [3/9] DeepSeek API Key (required for LLM grading): "
if "%DEEPSEEK_API_KEY%"=="" (
    echo   [WARN] No API Key provided. LLM grading will be unavailable!
    echo          You can set it later by editing .env file.
)

:: Redis Password
set "REDIS_PASSWORD="
set /p "REDIS_PASSWORD=  [4/9] Redis password [%MYSQL_PASSWORD%]: "
if "%REDIS_PASSWORD%"=="" set "REDIS_PASSWORD=%MYSQL_PASSWORD%"

:: Server Domain / IP
set "SERVER_DOMAIN="
set /p "SERVER_DOMAIN=  [5/9] Server domain or IP (e.g. example.com or 192.168.1.100) [localhost]: "
if "%SERVER_DOMAIN%"=="" set "SERVER_DOMAIN=localhost"

:: CORS Allowed Origins
set "CORS_ALLOWED_ORIGINS="
set /p "CORS_ALLOWED_ORIGINS=  [6/9] CORS allowed origins (comma-separated) [http://localhost,http://localhost:*,http://127.0.0.1,http://127.0.0.1:*]: "
if "%CORS_ALLOWED_ORIGINS%"=="" (
    set "CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:*,http://127.0.0.1,http://127.0.0.1:*"
)
if not "%SERVER_DOMAIN%"=="localhost" (
    set "CORS_ALLOWED_ORIGINS=!CORS_ALLOWED_ORIGINS!,http://!SERVER_DOMAIN!,http://!SERVER_DOMAIN!:*,https://!SERVER_DOMAIN!,https://!SERVER_DOMAIN!:*"
)

:: Enable Swagger
set "SWAGGER_ENABLED="
set /p "SWAGGER_ENABLED=  [7/9] Enable Swagger API docs in production? (true/false) [false]: "
if "%SWAGGER_ENABLED%"=="" set "SWAGGER_ENABLED=false"

:: Docker Mirror
set "DOCKER_MIRROR="
set /p "DOCKER_MIRROR=  [8/9] Docker mirror prefix for China (e.g. xw4fis2eywyg7vamgy.xuanyuan.run) [none]: "
if "%DOCKER_MIRROR%"=="" (
    set "DOCKER_MIRROR="
    echo   No Docker mirror configured. Images will be pulled from Docker Hub directly.
)

:: npm / Maven mirror
set "NPM_REGISTRY="
set "MAVEN_MIRROR="
if not "%DOCKER_MIRROR%"=="" (
    set "NPM_REGISTRY=https://registry.npmmirror.com"
    set "MAVEN_MIRROR=https://maven.aliyun.com/repository/public"
    echo   [Auto] npm registry: !NPM_REGISTRY!
    echo   [Auto] Maven mirror: !MAVEN_MIRROR!
) else (
    set /p "NPM_REGISTRY=  [9/9] npm registry [https://registry.npmjs.org]: "
    if "!NPM_REGISTRY!"=="" set "NPM_REGISTRY=https://registry.npmjs.org"
    set /p "MAVEN_MIRROR=       Maven mirror URL (leave empty for default) []: "
)

echo.
echo   Configuration summary:
echo   ------------------------------------
echo   MySQL Password:    ********
echo   JWT Secret:        ********
echo   DeepSeek API:      ********
echo   Redis Password:    ********
echo   Server Domain:     %SERVER_DOMAIN%
echo   CORS Origins:      %CORS_ALLOWED_ORIGINS%
echo   Swagger Enabled:   %SWAGGER_ENABLED%
echo   Docker Mirror:     %DOCKER_MIRROR%
echo   npm Registry:      %NPM_REGISTRY%
echo   Maven Mirror:      %MAVEN_MIRROR%
echo   ------------------------------------
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
echo [Step 3/6] Writing .env file...

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
echo.
echo SERVER_DOMAIN=%SERVER_DOMAIN%
echo CORS_ALLOWED_ORIGINS=%CORS_ALLOWED_ORIGINS%
echo SWAGGER_ENABLED=%SWAGGER_ENABLED%
echo.
echo NGINX_HTTP_PORT=8888
echo NGINX_HTTPS_PORT=443
echo SSL_CERT_DIR=./docker/nginx/ssl
echo.
echo HF_ENDPOINT=https://hf-mirror.com
echo.
echo DOCKER_MIRROR=%DOCKER_MIRROR%
echo NPM_REGISTRY=%NPM_REGISTRY%
echo MAVEN_MIRROR=%MAVEN_MIRROR%
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

:: Update nginx.conf server_name if not localhost
if not "%SERVER_DOMAIN%"=="localhost" (
    echo   [OK] Updating nginx.conf server_name...
    powershell -Command "(Get-Content 'docker\nginx\nginx.conf') -replace 'server_name localhost', 'server_name %SERVER_DOMAIN%' | Set-Content 'docker\nginx\nginx.conf'"
)

echo.

:: ============================================================
:: Step 4: Build
:: ============================================================
:step_build

:: Re-read mirror settings from .env when skipping reconfigure
if "%DOCKER_MIRROR%"=="" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if /i "%%a"=="DOCKER_MIRROR" set "DOCKER_MIRROR=%%b"
        if /i "%%a"=="NPM_REGISTRY" set "NPM_REGISTRY=%%b"
        if /i "%%a"=="MAVEN_MIRROR" set "MAVEN_MIRROR=%%b"
    )
)

echo [Step 4/6] Building project...
echo.

if "%HAS_NODE%"=="1" (
    echo   [4a] Building frontend...
    cd frontend
    if not exist "node_modules" (
        echo   Installing dependencies...
        if not "%NPM_REGISTRY%"=="" (
            call npm install --legacy-peer-deps --registry=%NPM_REGISTRY%
        ) else (
            call npm install --legacy-peer-deps
        )
        if %errorlevel% neq 0 (
            echo   [ERROR] npm install failed!
            cd ..
            pause
            exit /b 1
        )
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

    if not "%MAVEN_MIRROR%"=="" (
        call :write_maven_settings
        call mvn clean package -DskipTests -q -s settings-mirror.xml
        set "MVN_ERR=!errorlevel!"
        del settings-mirror.xml 2>nul
    ) else (
        call mvn clean package -DskipTests -q
        set "MVN_ERR=!errorlevel!"
    )

    if !MVN_ERR! neq 0 (
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
:: Step 5: Pull Docker images via mirror
:: ============================================================
echo [Step 5/6] Preparing Docker images...
echo.

if not "%DOCKER_MIRROR%"=="" (
    call :pull_mirror_images
) else (
    echo   No Docker mirror configured, images will be pulled directly.
)

:: ============================================================
:: Step 6: Start services
:: ============================================================
echo.
echo [Step 6/6] Starting services...
echo.

docker compose down >nul 2>&1

docker compose up -d
if %errorlevel% neq 0 (
    echo   [ERROR] Failed to start services!
    pause
    exit /b 1
)

echo   Waiting for MySQL initialization...
timeout /t 20 /nobreak >nul

echo.
echo  +============================================================+
echo  ^|            Setup Complete!                               ^|
echo  +============================================================+
echo.
echo   Access URLs:
echo     Frontend:    http://%SERVER_DOMAIN%:8888
echo     API Docs:    http://%SERVER_DOMAIN%:8888/doc.html
echo     Health:      http://%SERVER_DOMAIN%:8888/api/health
echo.
echo   Default accounts (PLEASE CHANGE PASSWORDS AFTER FIRST LOGIN!):
echo     Admin:       admin / 123456
echo     Teacher:     teacher / 123456
echo     Student:     student / 123456
echo.
echo   [IMPORTANT] Please change default passwords immediately!
echo.
echo   SSL/HTTPS:
echo     To enable HTTPS, place your certificate files in:
echo       docker/nginx/ssl/fullchain.pem
echo       docker/nginx/ssl/privkey.pem
echo     Then replace docker/nginx/nginx.conf with nginx-ssl.conf.template
echo.
echo   Useful commands:
echo     View logs:   docker-compose logs -f
echo     Stop:        stop.bat
echo     Rebuild:     rebuild.bat
echo.
pause
endlocal
exit /b 0

:: ============================================================
:: Subroutines
:: ============================================================

:write_maven_settings
(
    echo ^<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"^>
    echo   ^<mirrors^>
    echo     ^<mirror^>
    echo       ^<id^>aliyun-maven^</id^>
    echo       ^<mirrorOf^>central^</mirrorOf^>
    echo       ^<name^>Aliyun Maven Mirror^</name^>
    echo       ^<url^>%MAVEN_MIRROR%^</url^>
    echo     ^</mirror^>
    echo   ^</mirrors^>
    echo ^</settings^>
) > settings-mirror.xml
exit /b 0

:pull_mirror_images
echo   Pulling base images via mirror: %DOCKER_MIRROR%
for %%i in ("mysql:8.0" "redis:7.0-alpine" "nginx:1.25-alpine" "chromadb/chroma:0.4.24" "maven:3.9.8-eclipse-temurin-17" "eclipse-temurin:17-jre") do (
    echo     Pulling %%~i ...
    docker pull %DOCKER_MIRROR%/%%~i
    if !errorlevel! equ 0 (
        docker tag %DOCKER_MIRROR%/%%~i %%~i
    ) else (
        echo     [WARN] Failed to pull %%~i via mirror, will try direct.
    )
)
echo   Done.
exit /b 0
