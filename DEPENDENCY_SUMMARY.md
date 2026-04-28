# 项目依赖安装摘要

## ✅ 已完成的依赖安装

### 1. 前端依赖
**状态：✅ 已完成**
- **安装路径：** `d:\WorkSpace\Management_Sys\frontend\node_modules\`
- **包数量：** 112 个包
- **核心依赖：**
  - Vue 3.5.32
  - Vue Router 4.6.4
  - Pinia 2.3.1
  - Axios 1.15.0
  - Element Plus 2.13.7
  - Monaco Editor 0.43.0
  - ECharts 5.6.0
  - Vite 4.5.14
  - TypeScript 5.9.3

---

### 2. Python批阅引擎核心依赖
**状态：✅ 已完成**
- **虚拟环境路径：** `d:\WorkSpace\Management_Sys\grading-engine\venv\`
- **核心包列表：**
  - Celery 5.3.4 (异步任务)
  - Redis 5.0.1 (消息队列/缓存)
  - Requests 2.31.0 (HTTP客户端)
  - OpenAI 1.6.0 (LLM API)
  - PyYAML 6.0.1 (配置解析)
  - Docker 7.0.0 (沙箱管理)

---

## ⏳ 待安装的依赖（需要手动环境）

### 1. Python批阅引擎完整依赖
**状态：⏳ 等待C++ Build Tools**
- **缺失包：**
  - ChromaDB 0.4.21 (向量数据库)
  - Sentence-Transformers 2.2.2 (文本嵌入)

**前置要求：** 安装 Microsoft C++ Build Tools

---

### 2. 后端依赖
**状态：⏳ 等待Java/Maven**
- **前置要求：**
  - Java 17+
  - Maven 3.9.x

**安装命令（环境就绪后）：**
```powershell
cd backend
mvn clean install
```

---

### 3. Docker服务
**状态：⏳ 等待Docker Desktop**
- **前置要求：** Docker Desktop

**启动命令（环境就绪后）：**
```powershell
docker-compose up -d
```

---

## 📋 环境依赖安装指南

| 组件 | 状态 | 下载地址 |
|------|------|----------|
| Microsoft C++ Build Tools | ⏳ 待安装 | https://visualstudio.microsoft.com/visual-cpp-build-tools/ |
| Java 17 (OpenJDK) | ⏳ 待安装 | https://adoptium.net/ |
| Maven 3.9.x | ⏳ 待安装 | https://maven.apache.org/download.cgi |
| Docker Desktop | ⏳ 待安装 | https://www.docker.com/products/docker-desktop/ |

---

## 🚀 后续步骤

1. **安装 Microsoft C++ Build Tools**
2. **安装完整Python依赖：**
   ```powershell
   cd grading-engine
   .\venv\Scripts\Activate.ps1
   pip install -r requirements.txt
   ```
3. **安装Java 17和Maven**
4. **安装后端依赖：**
   ```powershell
   cd backend
   mvn clean install
   ```
5. **安装Docker Desktop**
6. **启动完整服务：**
   ```powershell
   docker-compose up -d
   ```
