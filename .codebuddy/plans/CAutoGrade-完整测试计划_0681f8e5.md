---
name: CAutoGrade-完整测试计划
overview: 为 CAutoGrade 项目创建完整的代码排查与测试计划，包含三个独立计划：前端测试计划（vitest + Vue Test Utils）、后端测试计划（JUnit 5 + Mockito + Spring Test）、组合测试计划（端到端 API 流程测试 + Docker 集成测试），覆盖 测试.txt 中记录的所有已知缺陷。
todos:
  - id: frontend-test-infra
    content: 搭建前端测试基础设施：安装 vitest/@vue/test-utils/jsdom/@pinia/testing，配置 vitest.config.ts 和 package.json test 脚本
    status: completed
  - id: frontend-api-tests
    content: 编写前端 API 层测试：在 src/__tests__/api/index.test.ts 中验证 18 组 API 函数的 URL、方法、参数格式
    status: completed
    dependencies:
      - frontend-test-infra
  - id: frontend-stores-tests
    content: 编写前端 Stores 测试：auth.test.ts 认证流程、assignment.test.ts 作业状态、notification.test.ts 通知状态
    status: completed
    dependencies:
      - frontend-test-infra
  - id: frontend-composables-tests
    content: 编写前端 Composables 测试：useCountdown 倒计时、usePermission 权限判定、usePolling 轮询逻辑
    status: completed
    dependencies:
      - frontend-test-infra
  - id: frontend-router-tests
    content: 编写前端路由守卫测试：token 缺失重定向、角色不匹配拦截、登录页豁免
    status: completed
    dependencies:
      - frontend-test-infra
  - id: backend-service-tests
    content: 补充后端未测试 Service：AnalyticsServiceTest、FileServiceTest、NotificationServiceTest、OperationLogServiceTest
    status: completed
  - id: backend-controller-tests
    content: 编写后端 Controller 集成测试：为 AuthController、CourseController、ClassController、SubmissionController、AdminController 编写 @WebMvcTest
    status: completed
  - id: backend-security-tests
    content: 补充后端 Security 层测试：JwtAuthenticationFilterTest 和 UserDetailsServiceImplTest
    status: completed
  - id: integration-plan-doc
    content: Use [skill:docx] 生成组合测试方案文档，包含三大角色端到端流程、前后端联调验证步骤和环境准备指南
    status: completed
    dependencies:
      - frontend-api-tests
      - frontend-router-tests
      - backend-controller-tests
      - backend-security-tests
  - id: defect-tracking-doc
    content: Use [skill:xlsx] 生成缺陷追踪表，逐条记录 11 个已知缺陷的排查策略、修复方向、状态跟踪
    status: completed
    dependencies:
      - backend-controller-tests
---

## 产品概述

为 CAutoGrade（C 语言自动批阅系统）创建完整的测试体系，覆盖前端、后端和组合测试三层，同时针对项目中 11 个已知缺陷设计专项排查用例，确保所有核心业务流程可验证、可回归。

## 三大测试计划

### 计划一：前端单元测试

从零搭建前端测试基础设施（vitest + @vue/test-utils + jsdom），为核心模块编写单元测试，覆盖 API 层、状态管理、路由守卫、组合式函数和关键组件。

### 计划二：后端测试补充

在现有 10 个 Service 测试的基础上，补齐 Controller 层集成测试（@WebMvcTest）、Security 层单元测试和未测试的 Service（AnalyticsService, FileService, NotificationService, OperationLogService），使测试覆盖率显著提升。

### 计划三：组合集成测试

设计前后端联调测试流程，验证用户核心操作链路（注册→登录→课程创建→班级管理→作业发布→代码提交→评阅审核）的完整性和正确性，并针对已知 11 个缺陷提供定向排查用例。

## 核心目标

1. **前端测试从 0 到 1**：搭建 vitest 框架，编写至少 15 个测试用例覆盖 API 层、Stores、Composables、路由守卫
2. **后端 Controller 层破冰**：为 5 个关键 Controller（AuthController, CourseController, ClassController, SubmissionController, AdminController）编写 @WebMvcTest 集成测试
3. **后端 Service 补齐**：为 4 个未测试 Service 编写单元测试
4. **Security 层覆盖**：为 JwtAuthenticationFilter 和 UserDetailsServiceImpl 编写测试
5. **缺陷排查测试**：针对已知 11 个缺陷，编写可执行的排查用测试用例，定位问题根因
6. **组合验证**：设计端到端用户流程测试方案，确保前后端协同正常

## 技术选型

### 前端测试栈

| 工具 | 版本 | 用途 |
| --- | --- | --- |
| vitest | ^1.x | 测试运行器，与 Vite 原生集成 |
| @vue/test-utils | ^2.x | Vue 组件挂载与交互测试 |
| jsdom | latest | 浏览器环境模拟 |
| @pinia/testing | ^0.x | Pinia Store 测试工具 |


### 后端测试栈（全部已有依赖）

| 工具 | 用途 |
| --- | --- |
| JUnit 5 + Mockito | 单元测试框架（已有） |
| @WebMvcTest | Spring MVC 层测试切片 |
| @MockBean / @InjectMocks | 依赖注入 Mock |
| spring-security-test | 安全测试支持（已有） |


## 实现方案

### 前端测试方案

1. **安装依赖**：vitest, @vue/test-utils, jsdom, @pinia/testing
2. **配置 vitest.config.ts**：继承 Vite 配置，配置 jsdom 环境，设置 @ 别名
3. **测试文件组织**：在 `frontend/src/__tests__/` 下按层级组织（api/, stores/, composables/, router/）
4. **优先级策略**：先从纯逻辑模块（API、Stores、Composables）开始，再覆盖组件

### 后端测试方案

1. **复用现有模式**：Service 测试遵循现有 `@ExtendWith(MockitoExtension.class)` + `@Mock` Mapper 模式
2. **Controller 测试**：使用 `@WebMvcTest` + `@MockBean` Service + `@Autowired MockMvc` + `@WithMockUser`
3. **Security 测试**：JwtAuthenticationFilter 使用 Mockito 模拟 HttpServletRequest/Response 链；UserDetailsServiceImpl Mock Mapper

### 组合测试方案

1. **API 层 Mock 拦截器测试**：验证前端 API 调用参数和后端返回数据的格式一致性
2. **端到端流程文档**：基于 Docker Compose 环境，按角色划分测试场景，对照已知缺陷逐项验证
3. **缺陷专项排查**：每个已知缺陷对应一条排查策略，从日志、网络请求和断点三个维度定位

## 目录结构

```
CAutoGrade/
├── frontend/
│   ├── vitest.config.ts                    # [NEW] vitest 配置文件，继承 Vite alias，配置 jsdom 环境
│   ├── src/__tests__/
│   │   ├── api/
│   │   │   └── index.test.ts              # [NEW] 18组API函数参数格式和返回值类型测试
│   │   ├── stores/
│   │   │   ├── auth.test.ts               # [NEW] 认证状态：登录/登出/token 持久化
│   │   │   ├── assignment.test.ts         # [NEW] 作业状态更新
│   │   │   └── notification.test.ts       # [NEW] 通知状态：未读数、标记已读
│   │   ├── composables/
│   │   │   ├── useCountdown.test.ts       # [NEW] 倒计时：正常计时、过期、边界值
│   │   │   ├── usePermission.test.ts      # [NEW] 权限校验：三角色权限判定
│   │   │   └── usePolling.test.ts         # [NEW] 轮询：启动、停止、回调执行
│   │   └── router/
│   │       └── index.test.ts              # [NEW] 路由守卫：token缺失重定向、角色不匹配拦截
│   └── package.json                        # [MODIFY] 新增 vitest, @vue/test-utils, jsdom, @pinia/testing 依赖和 test 脚本
│
├── backend/src/test/java/com/autograding/
│   ├── controller/                         # [NEW] Controller 测试目录（全部新建）
│   │   ├── AuthControllerTest.java        # [NEW] 注册/登录接口：请求参数校验、返回 Result 格式、JWT token 生成
│   │   ├── CourseControllerTest.java      # [NEW] 课程 CRUD 接口：GET/POST/PUT/DELETE，教师权限校验
│   │   ├── ClassControllerTest.java       # [NEW] 班级管理接口：创建/加入/添加学生/移除学生
│   │   ├── SubmissionControllerTest.java  # [NEW] 提交代码接口：提交/查看/查询评分结果
│   │   └── AdminControllerTest.java       # [NEW] 管理端接口：用户管理 CRUD，管理员权限校验
│   ├── service/
│   │   ├── AnalyticsServiceTest.java      # [NEW] 学情分析 Service：班级/学生/作业/题目维度数据分析
│   │   ├── FileServiceTest.java           # [NEW] 文件服务：成绩导出 Excel 生成
│   │   ├── NotificationServiceTest.java   # [NEW] 通知服务：获取/标记已读/未读数统计
│   │   └── OperationLogServiceTest.java   # [NEW] 操作日志：日志查询和分页
│   └── security/
│       ├── JwtAuthenticationFilterTest.java   # [NEW] JWT过滤器：有效token放行、无效/无token返回401
│       └── UserDetailsServiceImplTest.java    # [NEW] 用户详情加载：用户名查询、角色映射
│
├── docs/
│   └── test-plan-integration.md            # [NEW] 组合测试方案文档：端到端流程、缺陷排查用例、环境准备
│
└── docs/
    └── defect-tracking.md                  # [NEW] 缺陷追踪表：11个已知缺陷的排查策略和修复方向
```

## 关键代码结构

### 前端 vitest.config.ts 配置要点

- 环境：`environment: 'jsdom'`
- 别名：继承 `vite.config.ts` 的 `@` 指向 `src/`
- 测试文件匹配：`src/__tests__/**/*.test.ts`

### 后端 Controller 测试模式（@WebMvcTest）

```java
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // 绕过 Security 过滤器
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService; // 仅 Mock 业务层
}
```

## 实现要点

### 前端测试

- **API 层测试**：Mock axios 实例，验证每个 API 函数的 URL、请求方法、参数格式是否正确
- **Store 测试**：使用 `setActivePinia(createTestingPinia())` 创建隔离的 Pinia 实例
- **Composable 测试**：使用 `vi.useFakeTimers()` 模拟定时器验证 useCountdown 和 usePolling

### 后端测试

- **遵循现有模式**：所有 Service 测试继承 `@ExtendWith(MockitoExtension.class)` 模式
- **Controller 层**：使用 `@WebMvcTest` + `@MockBean`，禁用 JWT 过滤器（addFilters=false）专注接口逻辑
- **Security 层**：JwtAuthenticationFilter 使用 `MockHttpServletRequest/Response` 链测试

### 组合测试

- **全局验证目录**：涵盖三大角色 + 完整业务链路
- **缺陷专项**：每个已知缺陷对应具体的排查步骤（前端请求参数检查 → 后端日志定位 → 数据库数据验证）

## 可用扩展

### Skill

- **docx**
- 用途：生成组合测试方案文档（.docx 格式），包含端到端测试流程、缺陷排查用例和环境准备指南
- 预期结果：产出 `docs/test-plan-integration.docx`，结构清晰、可直接用于团队执行测试

- **xlsx**
- 用途：生成缺陷追踪表格（.xlsx 格式），逐条列出 11 个已知缺陷的状态、排查策略、修复方向和责任人
- 预期结果：产出 `docs/defect-tracking.xlsx`，支持筛选和状态更新

### SubAgent

- **code-explorer**
- 用途：在编写测试前快速定位待测模块的关键代码逻辑和依赖关系，确保测试 Mock 对象准确
- 预期结果：为每个测试提供准确的依赖注入关系和需要覆盖的边界条件列表