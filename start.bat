@echo off
chcp 65001 >nul
echo ========================================
echo   C语言自动批阅系统 - 启动脚本
echo ========================================
echo.

cd /d "%~dp0"

echo 正在启动 Docker 服务...
docker-compose up -d

echo.
echo 等待服务启动...
timeout /t 5 /nobreak >nul

echo.
echo ========================================
echo   服务已启动！
echo ========================================
echo.
echo 访问地址:
echo   前端: http://localhost
echo   API文档: http://localhost/doc.html
echo.
echo 初始账号:
echo   管理员: admin / admin123
echo   教师: teacher / 123456
echo   学生: student / 123456
echo.
echo 如需查看日志: docker-compose logs -f
echo 如需停止服务: docker-compose down
echo.
pause
