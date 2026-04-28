# C 语言程序设计 — 智能自动批阅系统

基于 LLM 的 C 语言编程作业自动批阅系统，支持代码提交、三层评阅（静态分析 + RAG 增强 + LLM 评估）、教师审核、学情分析等功能。

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
     │  (port 3000│                      │  (port 8080│
     │   dev)     │                      │            │
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
cd Management_Sys
```

### 2. 启动基础设施（MySQL + Redis + ChromaDB）

```bash
docker-compose up -d mysql redis chromadb
```

### 3. 启动后端

```bash
cd backend
# 开发环境（使用本地 MySQL/Redis）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或 Docker 方式
docker-compose up -d backend
```

后端启动后访问：
- API 文档：http://localhost:8080/doc.html
- 默认管理员：`admin` / `admin123`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev        # 开发模式，端口 3000
```

### 5. 启动批阅 Worker

```bash
cd grading-engine

# Windows（PowerShell）
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Linux / macOS
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# 设置 API Key
# Windows: $env:DEEPSEEK_API_KEY="your-key"
# Linux:   export DEEPSEEK_API_KEY="your-key"

# 启动 Worker
celery -A celery_app worker -Q grading_queue --loglevel=info --concurrency=4
```

### 6. 一键启动全部服务

```bash
# 所有服务
docker-compose up -d

# 开发环境（仅数据库）
docker-compose -f docker-compose.dev.yml up -d
```

## 项目结构

```
Management_Sys/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/autograding/
│   │   ├── common/                   # Result、BusinessException、GlobalExceptionHandler
│   │   ├── config/                   # SecurityConfig、RedisConfig、AsyncConfig
│   │   ├── controller/               # 15 个 REST Controller
│   │   ├── dto/                      # 请求/响应 DTO
│   │   ├── entity/                   # 13 个实体（JPA + MyBatis-Plus 双注解）
│   │   ├── mapper/                   # MyBatis-Plus BaseMapper
│   │   ├── scheduler/                # 定时任务（截止提醒、逾期标记、归档）
│   │   ├── security/                 # JWT 认证过滤器 + SecurityUtils
│   │   ├── service/                  # 业务逻辑层
│   │   └── repository/               # JPA Repository
│   └── src/main/resources/
│       ├── application.yml           # 主配置
│       ├── application-dev.yml       # 开发环境
│       └── application-prod.yml      # 生产环境
│
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # Axios 封装 + API 函数
│       ├── components/               # 通用组件
│       │   ├── CodeEditor/           # Monaco Editor 封装
│       │   ├── GradingResult/        # 批阅结果展示
│       │   ├── NotificationBell/     # 通知铃铛
│       │   └── Common/               # EmptyState、Pagination
│       ├── composables/              # usePolling、useCountdown、usePermission
│       ├── router/                   # Vue Router（角色路由守卫）
│       ├── stores/                   # Pinia 状态管理
│       ├── styles/                   # 全局 CSS 变量
│       ├── types/                    # TypeScript 类型定义
│       ├── utils/                    # 工具函数
│       └── views/                    # 页面组件
│           ├── student/              # 学生端（8 页面）
│           ├── teacher/              # 教师端（10 页面）
│           ├── admin/                # 管理员端（4 页面）
│           ├── auth/                 # 登录/注册
│           └── common/               # 404
│
├── grading-engine/                   # Python 批阅引擎
│   ├── core/                         # 核心模块
│   │   ├── dispatcher.py             # 批阅调度器（三层流水线）
│   │   ├── llm_service.py            # LLM API 调用封装
│   │   ├── static_analyzer.py        # 静态分析器 + 测试用例执行
│   │   ├── sandbox.py                # Docker 沙箱编译执行
│   │   ├── scorer.py                 # 规则评分器（兜底）
│   │   └── rag_service.py            # RAG 检索服务
│   ├── prompts/                      # LLM Prompts
│   │   ├── system_prompt_template.py
│   │   ├── few_shot_examples.py
│   │   └── output_schema.py
│   ├── rag/                          # RAG 知识库
│   │   ├── embedding.py              # 文本向量化
│   │   ├── vector_store.py           # ChromaDB 操作
│   │   ├── knowledge_loader.py       # 知识库加载
│   │   ├── retriever.py              # 混合检索
│   │   └── data/                     # 知识库 JSON（评分标准、知识点、常见错误）
│   ├── tasks/                        # Celery 异步任务
│   │   ├── grading_task.py           # 批阅任务
│   │   ├── notification_task.py      # 通知回调
│   │   └── consistency_check_task.py # 一致性检测
│   ├── utils/                        # 工具
│   │   ├── code_parser.py            # 代码解析
│   │   ├── result_formatter.py       # 结果格式化
│   │   └── retry.py                  # 重试策略
│   ├── tests/golden_standard/        # 黄金标准测试数据
│   ├── config.yaml                   # 运行时配置
│   ├── celery_app.py                 # Celery 应用
│   └── requirements.txt
│
├── docker/                           # Docker 配置
│   ├── mysql/init.sql                # 数据库初始化（13 表 + 默认用户）
│   ├── redis/redis.conf              # Redis 配置
│   ├── nginx/nginx.conf              # Nginx 反向代理
│   └── sandbox/Dockerfile.sandbox    # C 代码沙箱镜像
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

## API 概览

所有接口返回统一格式 `{ code: 200, msg: "成功", data: ... }`，路径前缀 `/api`。

| 资源 | 端点 | 说明 |
|------|------|------|
| 认证 | `/api/auth/*` | 登录、注册（无需认证） |
| 用户 | `/api/users/*` | 个人信息、密码修改 |
| 课程 | `/api/courses/*` | 课程 CRUD |
| 班级 | `/api/classes/*` | 班级管理、学生管理 |
| 作业 | `/api/assignments/*` | 作业发布、发布/归档 |
| 题目 | `/api/problems/*` | 题库管理 |
| 测试用例 | `/api/test-cases/*` | 用例配置 |
| 提交 | `/api/submissions/*` | 代码提交、批阅结果查询 |
| 批阅 | `/api/gradings/*` | 待审列表、人工审核 |
| 通知 | `/api/notifications/*` | 消息列表、已读 |
| 公告 | `/api/announcements/*` | 公告 CRUD |
| 学情 | `/api/analytics/*` | 成绩统计、知识点分析 |
| 文件 | `/api/files/*` | 上传、下载 |

## 配置说明

### 后端配置 (`backend/src/main/resources/application.yml`)

```yaml
jwt:
  secret: your-jwt-secret-key    # JWT 签名密钥
  expiration: 86400000            # Token 有效期（毫秒，默认 24h）

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/autograding
    username: root
    password: 123456
```

### 批阅引擎配置 (`grading-engine/config.yaml`)

```yaml
redis:
  url: redis://localhost:6379/1

llm:
  provider: deepseek               # deepseek / openai / zhipu / qwen
  base_url: https://api.deepseek.com/v1
  model: deepseek-chat
  timeout: 30

runtime:
  compile_timeout_seconds: 5       # 编译超时
  run_timeout_seconds: 5           # 运行超时
  memory_limit_mb: 256             # 内存限制
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | — |
| `MYSQL_PASSWORD` | MySQL root 密码 | `123456` |
| `SPRING_PROFILES_ACTIVE` | Spring 环境 | `dev` |

## 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| 截止提醒 | 每 15 分钟 | 检查 24h / 1h 内截止的作业，推送通知 |
| 逾期标记 | 每 5 分钟 | 标记截止时间后提交的代码为迟交 |
| 超时检测 | 每 30 分钟 | 检测 PENDING 超过 30 分钟的批阅任务 |
| 过期归档 | 每天凌晨 2:00 | 将截止超过 7 天的作业归档 |

## 沙箱安全

代码执行在 Docker 隔离环境中进行：
- `--network none`：禁止网络访问
- `--memory=256m`：内存限制
- `--cpus=1`：CPU 限制
- `ulimit -t 5`：CPU 时间限制（5 秒）
- 编译和运行均设置超时，防止死循环或恶意代码
- 执行完成后自动清理临时目录

## 开发计划

- [x] 后端核心（实体、控制器、服务、安全）
- [x] 前端三端页面（学生/教师/管理员）
- [x] 批阅引擎（静态分析 + LLM + RAG）
- [x] 定时任务与通知
- [x] 学情分析与成绩导出
- [x] 单元测试（6/6 通过）
- [x] Docker 部署配置
- [ ] CI/CD 流水线
- [ ] WebSocket 实时批阅状态推送
- [ ] 代码查重检测
- [ ] 更多 LLM 提供商适配
