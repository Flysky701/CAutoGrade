@echo off
chcp 936 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ========================================
echo   C AutoGrade - Rebuild Script
echo ========================================
echo.

:: ============================================================
:: Read configuration from .env
:: ============================================================
if not exist ".env" (
    echo [ERROR] .env file not found!
    echo   Please run setup.bat first.
    echo.
    pause
    exit /b 1
)

set "DOCKER_MIRROR="
set "NPM_REGISTRY="
set "MAVEN_MIRROR="

for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
    if /i "%%a"=="DOCKER_MIRROR" set "DOCKER_MIRROR=%%b"
    if /i "%%a"=="NPM_REGISTRY" set "NPM_REGISTRY=%%b"
    if /i "%%a"=="MAVEN_MIRROR" set "MAVEN_MIRROR=%%b"
)

if not "%DOCKER_MIRROR%"=="" (
    echo [Config] Docker mirror: %DOCKER_MIRROR%
)
if not "%NPM_REGISTRY%"=="" (
    echo [Config] npm registry: %NPM_REGISTRY%
)
if not "%MAVEN_MIRROR%"=="" (
    echo [Config] Maven mirror: %MAVEN_MIRROR%
)
echo.

:: ============================================================
:: Check prerequisites
:: ============================================================
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Please install it first.
    pause
    exit /b 1
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Java not found. Backend build will be skipped.
    set "HAS_JAVA=0"
) else (
    set "HAS_JAVA=1"
)

:: ============================================================
:: Step 1: Frontend
:: ============================================================
echo [1/4] Installing frontend dependencies...
cd frontend
if not exist "node_modules" (
    if not "%NPM_REGISTRY%"=="" (
        call npm install --registry=%NPM_REGISTRY%
    ) else (
        call npm install
    )
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed
        cd ..
        pause
        exit /b 1
    )
) else (
    echo   Dependencies already installed, skipping.
)

echo.
echo [2/4] Building frontend...
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] Frontend build failed
    cd ..
    pause
    exit /b 1
)
cd ..

:: ============================================================
:: Step 2: Backend
:: ============================================================
echo.
echo [3/4] Building backend...
if "%HAS_JAVA%"=="0" (
    echo   Skipping backend build ^(Java not found^).
    if not exist "backend\target\backend-1.0.0-SNAPSHOT.jar" (
        echo [ERROR] backend\target\*.jar not found! Build backend manually first.
        pause
        exit /b 1
    )
) else (
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
        echo [ERROR] Backend build failed
        cd ..
        pause
        exit /b 1
    )
    cd ..
)

:: ============================================================
:: Step 3: Docker images
:: ============================================================
echo.
echo [4/4] Rebuilding Docker images...

if not "%DOCKER_MIRROR%"=="" (
    call :pull_mirror_images
)

echo   Building backend image...
docker compose build --no-cache backend
if %errorlevel% neq 0 (
    echo [ERROR] Backend image build failed
    pause
    exit /b 1
)

echo   Building grading-worker image...
docker compose build grading-worker
if %errorlevel% neq 0 (
    echo [ERROR] Grading-worker image build failed
    pause
    exit /b 1
)

:: ============================================================
:: Step 4: Start
:: ============================================================
echo.
echo   Starting all services...
docker compose up -d
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
echo   Frontend:  http://localhost:8888
echo   API Docs:  http://localhost:8888/doc.html
echo.
echo   Press Ctrl+Shift+R to force refresh browser cache.
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
echo   Pulling base images via mirror...
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
