# CAutoGrade — 基于 LLM 的 C 语言作业自动批阅系统

基于大语言模型的 C 语言编程作业自动批阅系统，采用**静态分析 + RAG 增强 + LLM 评估**三层联动架构，实现对学生提交的 C 语言代码的自动分析、评分与反馈生成。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2、Java 17、MyBatis-Plus 3.5、Spring Security + JWT |
| 前端 | Vue 3、TypeScript、Vite 4、Element Plus、Monaco Editor、ECharts |
| 批阅引擎 | Python 3.11+、Celery、Redis、Docker SDK、ChromaDB |
| LLM | DeepSeek API（可切换 OpenAI / GLM / Qwen） |
| 基础设施 | Docker Compose（MySQL 8.0、Redis 7.0、ChromaDB、Nginx） |

## 功能概览

### 学生端
- 浏览课程与作业列表，查看截止倒计时
- Monaco Editor 在线编写/提交 C 代码
- 查看三维度批阅结果（正确性 / 规范性 / 效率）及逐行批注
- 查看提交历史和批阅状态

### 教师端
- 课程与班级管理（创建班级、添加学生、复制选课码）
- 题库管理（创建题目、配置测试用例、设置难度和知识点标签）
- 作业管理（三步向导：基本信息 → 选题 → 指定班级）
- 批阅审核（查看 AI 评分详情、人工修正分数、批量通过）
- 学情分析（成绩分布直方图、知识点掌握度雷达图、易错 Top 10）
- 公告管理（发布/编辑/置顶/删除）
- 成绩导出（Excel / CSV）

### 管理员端
- 用户管理（角色分配、状态控制）
- 系统配置（LLM 参数、沙箱策略、评分阈值）
- 操作日志

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                     Nginx (port 80)                      │
│                 前端静态资源 + API 反向代理                │
└──────────┬──────────────────────────────────┬───────────┘
           │                                  │
     ┌─────▼─────┐                      ┌─────▼─────┐
     │  Frontend  │                      │  Backend   │
     │  Vue 3     │──── HTTP/REST ─────▶│  Spring    │
     │  Vite      │                      │  Boot 3.2  │
     └───────────┘                      └──┬───┬───┬─┘
                                           │   │   │
                              ┌────────────┘   │   └────────────┐
                              ▼                 ▼                ▼
                       ┌──────────┐    ┌──────────┐    ┌──────────────┐
                       │  MySQL   │    │  Redis   │    │  Grading      │
                       │  8.0     │    │  7.0     │    │  Worker       │
                       │  (数据)  │    │(缓存/消息)│    │  (Celery)     │
                       └──────────┘    └──────────┘    └──┬──┬──┬──┬──┘
                                                          │  │  │  │
                                           ┌──────────────┘  │  │  └──────────┐
                                           ▼                  ▼  ▼              ▼
                                    ┌────────────┐   ┌────────────┐   ┌──────────────┐
                                    │  Docker     │   │  ChromaDB  │   │  LLM API     │
                                    │  Sandbox    │   │  向量数据库 │   │  (DeepSeek/  │
                                    │  (gcc编译)  │   │  (RAG知识库)│   │   OpenAI等)  │
                                    └────────────┘   └────────────┘   └──────────────┘
```

## 评阅流水线

```
学生代码
  │
  ▼
┌─────────────────┐
│ Layer 1: 静态分析 │  Docker 沙箱中 gcc 编译 + 运行测试用例
│                 │  检查不安全函数、数组越界、内存泄漏等
└────────┬────────┘
         ▼
┌─────────────────┐
│ Layer 2: RAG 增强 │  ChromaDB 向量检索课程知识点 + 评分标准
│                 │  混合检索（向量相似度 + 关键词匹配）
└────────┬────────┘
         ▼
┌─────────────────┐
│ Layer 3: LLM 评估│  DeepSeek 结构化评分（JSON 输出）
│                 │  正确性(60) + 规范性(20) + 效率(20)
└────────┬────────┘
         ▼
    ┌────┴────┐
    │ 偏差过大？│──是──▶ 规则评分器兜底
    └────┬────┘
         │否
         ▼
   结构化批阅结果
   （总分 + 逐行批注 + 改进建议 + 测试用例通过情况）
```

## 快速开始

### 前置要求

- JDK 17+
- Node.js 18+
- Python 3.11+
- Docker & Docker Compose
- DeepSeek API Key（或其他兼容 OpenAI 接口的 API Key）

### 1. 克隆项目

```bash
git clone <repo-url>
cd CAutoGrade
```

### 2. 配置环境变量

```bash
# 必须设置的环境变量
export MYSQL_PASSWORD=your_mysql_password
export JWT_SECRET=your_jwt_secret_at_least_32_bytes_long
export DEEPSEEK_API_KEY=your_deepseek_api_key

# 可选环境变量
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USER=root
export REDIS_PASSWORD=your_redis_password
```

### 3. 启动基础设施（MySQL + Redis + ChromaDB）

```bash
docker-compose up -d mysql redis chromadb
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动后访问 API 文档：http://localhost:8080/doc.html

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 6. 启动批阅 Worker

```bash
cd grading-engine
pip install -r requirements.txt

# 设置 API Key
export DEEPSEEK_API_KEY="your-key"

# 启动 Worker
celery -A celery_app worker -Q grading_queue --loglevel=info --concurrency=4
```

### 7. 一键启动全部服务

```bash
# 所有服务
docker-compose up -d

# 开发环境（仅数据库）
docker-compose -f docker-compose.dev.yml up -d
```

## 项目结构

```
CAutoGrade/
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/autograding/
│       ├── common/                   # Result、BusinessException、GlobalExceptionHandler
│       ├── config/                   # SecurityConfig、RedisConfig、AsyncConfig
│       ├── controller/               # 15 个 REST Controller
│       ├── dto/                      # 请求/响应 DTO
│       ├── entity/                   # 14 个实体（MyBatis-Plus）
│       ├── mapper/                   # MyBatis-Plus BaseMapper
│       ├── scheduler/                # 定时任务
│       ├── security/                 # JWT 认证过滤器 + SecurityUtils
│       └── service/                  # 业务逻辑层
│
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # Axios 封装 + API 函数
│       ├── components/               # 通用组件
│       ├── composables/              # usePolling、useCountdown、usePermission
│       ├── router/                   # Vue Router（角色路由守卫）
│       ├── stores/                   # Pinia 状态管理
│       └── views/                    # 页面组件
│           ├── student/              # 学生端
│           ├── teacher/              # 教师端
│           ├── admin/                # 管理员端
│           └── auth/                 # 登录/注册
│
├── grading-engine/                   # Python 批阅引擎
│   ├── core/                         # 核心模块（dispatcher、llm_service、static_analyzer 等）
│   ├── prompts/                      # LLM Prompts
│   ├── rag/                          # RAG 知识库
│   ├── tasks/                        # Celery 异步任务
│   └── utils/                        # 工具函数
│
├── docker/                           # Docker 配置
│   ├── mysql/init.sql                # 数据库初始化
│   ├── redis/redis.conf              # Redis 配置
│   ├── nginx/nginx.conf              # Nginx 反向代理
│   └── sandbox/Dockerfile.sandbox    # C 代码沙箱镜像
│
├── docs/                             # 项目文档
│   ├── README.md                     # 项目总览
│   ├── architecture.md               # 系统架构设计
│   ├── sql-schema.md                 # 数据库关系文档
│   └── uml.md                        # UML 设计文档
│
├── docker-compose.yml                # 完整服务编排
└── docker-compose.dev.yml            # 开发环境（仅 DB）
```

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `user` | 用户（学生/教师/管理员） |
| `course` | 课程 |
| `class` | 班级（含选课码） |
| `class_student` | 班级-学生关联 |
| `assignment` | 作业（含截止时间、类型、状态） |
| `assignment_problem` | 作业-题目关联 |
| `problem` | 题目（含难度、知识点标签） |
| `test_case` | 测试用例（含权重、隐藏标记） |
| `submission` | 提交记录（含代码内容、是否迟交） |
| `grading_result` | 批阅结果（三维度分 + JSON 反馈） |
| `notification` | 通知 |
| `announcement` | 公告 |
| `operation_log` | 操作日志 |
| `system_config` | 系统配置 |

## 环境变量

| 变量 | 必需 | 说明 |
|------|------|------|
| `MYSQL_PASSWORD` | 是 | MySQL root 密码 |
| `JWT_SECRET` | 是 | JWT 签名密钥（至少 32 字节） |
| `DEEPSEEK_API_KEY` | 是 | DeepSeek API 密钥 |
| `MYSQL_HOST` | 否 | MySQL 主机地址（默认 localhost） |
| `MYSQL_PORT` | 否 | MySQL 端口（默认 3306） |
| `MYSQL_USER` | 否 | MySQL 用户名（默认 root） |
| `REDIS_PASSWORD` | 否 | Redis 密码 |

## 沙箱安全

代码执行在 Docker 隔离环境中进行：
- `--network none`：禁止网络访问
- `--memory=256m`：内存限制
- `--cpus=1`：CPU 限制
- `ulimit -t 5`：CPU 时间限制（5 秒）
- 编译和运行均设置超时，防止死循环或恶意代码
- 执行完成后自动清理临时目录

## 测试

```bash
# 后端测试（JUnit 5 + Mockito）
cd backend && mvn test

# 前端测试（Vitest）
cd frontend && npm run test

# 评分引擎测试（pytest）
cd grading-engine && python -m pytest tests/ -v
```

## 许可证

本项目仅供学习研究使用。
