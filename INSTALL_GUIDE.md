# 项目环境安装指南

## 📋 环境检查结果

### ✅ 已就绪环境
- Node.js v24.12.0
- npm 11.6.2
- Python 3.14.2

### ❌ 需要手动安装的环境
- **Java 17+**
- **Maven**
- **Docker Desktop**
- **Microsoft C++ Build Tools** (用于Python编译依赖)

---

## 🔧 手动安装步骤

### 1. 安装 Microsoft C++ Build Tools (Python依赖)

**下载地址：** https://visualstudio.microsoft.com/visual-cpp-build-tools/

**安装步骤：**
1. 下载并运行 Visual Studio Build Tools
2. 安装时选择 **"使用C++的桌面开发"** (Desktop development with C++)
3. 确保勾选以下组件：
   - MSVC v143 - VS 2022 C++ x64/x86 生成工具
   - Windows 10 SDK (或 Windows 11 SDK)
4. 完成安装后重启电脑

---

### 2. 安装 Java 17+

**推荐版本：** OpenJDK 17 (LTS)

**下载地址：**
- Oracle JDK: https://www.oracle.com/java/technologies/downloads/#java17
- 或使用 Eclipse Temurin: https://adoptium.net/

**安装步骤：**
1. 下载 Windows x64 Installer
2. 运行安装程序，按默认设置安装
3. 验证安装：打开新的 PowerShell，运行：
   ```powershell
   java -version
   ```

---

### 3. 安装 Maven

**下载地址：** https://maven.apache.org/download.cgi

**安装步骤：**
1. 下载 `apache-maven-3.9.x-bin.zip`
2. 解压到：`C:\Program Files\Apache\maven`
3. 配置环境变量：
   - 新建系统变量 `MAVEN_HOME` = `C:\Program Files\Apache\maven`
   - 在 `Path` 变量添加：`%MAVEN_HOME%\bin`
4. 验证安装：
   ```powershell
   mvn -version
   ```

---

### 4. 安装 Docker Desktop

**下载地址：** https://www.docker.com/products/docker-desktop/

**安装步骤：**
1. 下载 Docker Desktop for Windows
2. 运行安装程序
3. 安装完成后启动 Docker Desktop
4. 验证安装：
   ```powershell
   docker --version
   docker-compose --version
   ```

---

## 📦 完成手动安装后继续

安装完上述环境后，运行以下命令：

### 安装 Python 批阅引擎完整依赖：

```powershell
cd grading-engine
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

### 安装后端依赖：

```powershell
cd backend
mvn clean install
```

### 验证 Docker Compose 启动完整环境：

```powershell
docker-compose up -d
```

---

## 🎯 当前已完成

- ✅ 前端依赖已安装 (112个包)
- ✅ Python 虚拟环境已创建 (grading-engine\venv)
