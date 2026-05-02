@echo off
chcp 936 >nul
echo ========================================
echo   C AutoGrade - Stop Script
echo ========================================
echo.

cd /d "%~dp0"

echo Stopping all Docker services...
docker-compose down

echo.
echo ========================================
echo   All services stopped.
echo ========================================
echo.
echo To restart: start.bat
echo.
pause
