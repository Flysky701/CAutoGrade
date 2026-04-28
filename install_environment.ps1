# 项目环境安装脚本
# 按照安装规范执行环境安装

Write-Host "=== 项目环境安装开始 ==="

# 检查E盘目录结构
Write-Host "检查并创建目录结构..."
New-Item -Path 'E:\Dependencies\Runtimes' -ItemType Directory -Force
New-Item -Path 'E:\Dependencies\PackageManagers' -ItemType Directory -Force
New-Item -Path 'E:\Services\Databases' -ItemType Directory -Force
New-Item -Path 'E:\Services\MessageQueues' -ItemType Directory -Force
New-Item -Path 'E:\CLI' -ItemType Directory -Force

Write-Host "目录结构创建完成"

# 安装指南
Write-Host ""
Write-Host "=== 安装指南 ==="
Write-Host ""
Write-Host "1. 安装 Microsoft C++ Build Tools (Python依赖)"
Write-Host "   下载地址: https://visualstudio.microsoft.com/visual-cpp-build-tools/"
Write-Host "   安装选项: 选择 '使用C++的桌面开发'"
Write-Host "   必需组件: MSVC v143 生成工具 + Windows 10/11 SDK"
Write-Host ""
Write-Host "2. 安装 Java 17 (OpenJDK)"
Write-Host "   下载地址: https://adoptium.net/"
Write-Host "   版本: Eclipse Temurin 17 (LTS)"
Write-Host "   安装路径: E:\Dependencies\Runtimes\Java17"
Write-Host ""
Write-Host "3. 安装 Maven"
Write-Host "   下载地址: https://maven.apache.org/download.cgi"
Write-Host "   版本: 3.9.x"
Write-Host "   安装路径: E:\Dependencies\PackageManagers\Maven"
Write-Host ""
Write-Host "4. 安装 Docker Desktop"
Write-Host "   下载地址: https://www.docker.com/products/docker-desktop/"
Write-Host "   安装后启动并登录Docker账号"
Write-Host ""
Write-Host "=== 环境变量配置 ==="
Write-Host ""
Write-Host "安装完成后，需要配置以下环境变量:"
Write-Host ""
Write-Host "1. JAVA_HOME = E:\Dependencies\Runtimes\Java17"
Write-Host "2. MAVEN_HOME = E:\Dependencies\PackageManagers\Maven"
Write-Host "3. 在 Path 中添加:"
Write-Host "   %JAVA_HOME%\bin"
Write-Host "   %MAVEN_HOME%\bin"
Write-Host ""
Write-Host "=== 验证安装 ==="
Write-Host ""
Write-Host "安装完成后运行以下命令验证:"
Write-Host "   java -version"
Write-Host "   mvn -version"
Write-Host "   docker --version"
Write-Host ""
Write-Host "=== 安装脚本结束 ==="
