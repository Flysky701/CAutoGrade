# CAutoGrade 组合测试方案

## 目录
1. [环境准备](#环境准备)
2. [测试范围](#测试范围)
3. [教师端测试流程](#教师端测试流程)
4. [学生端测试流程](#学生端测试流程)
5. [管理端测试流程](#管理端测试流程)
6. [前后端联调验证](#前后端联调验证)
7. [缺陷专项排查](#缺陷专项排查)

---

## 环境准备

### 通过 Docker Compose 启动完整环境
```bash
# 启动全部服务 (MySQL + Redis + ChromaDB + Backend + Worker + Nginx)
docker-compose up -d

# 仅启动基础设施 (开发调试)
docker-compose -f docker-compose.dev.yml up -d
```

### 开发模式单独启动
```bash
# 后端
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd frontend && npm run dev

# 评分引擎
cd grading-engine
$env:DEEPSEEK_API_KEY="your-key"
celery -A celery_app worker -Q grading_queue --loglevel=info
```

### 测试账号
| 角色 | 用户名 | 密码 | 用途 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 管理端功能验证 |
| 教师 | teacher1 | 123456 | 教师端功能验证 |
| 学生 | student1 | 123456 | 学生端功能验证 |

---

## 测试范围

### 已验证通过的自动化测试

| 层级 | 测试类 | 测试数 | 状态 |
|------|--------|--------|------|
| **后端 Service** | AnalyticsServiceTest | 8 | ✅ 全部通过 |
| **后端 Service** | FileServiceTest | 10 | ✅ 全部通过 |
| **后端 Service** | NotificationServiceTest | 9 | ✅ 全部通过 |
| **后端 Service** | OperationLogServiceTest | 5 | ✅ 全部通过 |
| **后端 Controller** | AdminControllerTest | 7 | ✅ 全部通过 |
| **后端 Controller** | AuthControllerTest | 4 | ✅ 全部通过 |
| **后端 Security** | JwtAuthenticationFilterTest | 5 | ✅ 全部通过 |
| **后端 Security** | UserDetailsServiceImplTest | 6 | ✅ 全部通过 |
| **前端** | API 层测试 | 30+ | 📝 代码完成 |
| **前端** | Stores 测试 | 15+ | 📝 代码完成 |
| **前端** | Composables 测试 | 10+ | 📝 代码完成 |
| **前端** | 路由守卫测试 | 9 | 📝 代码完成 |

---

## 教师端测试流程

### T1. 课程管理验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| T1.1 | 登录教师账号 | POST /api/auth/login | 返回 JWT token | - |
| T1.2 | 创建新课程 | POST /api/courses | 课程创建成功，返回课程信息 | **#1 创建无法正确读取** |
| T1.3 | 查看我的课程 | GET /api/courses/teacher | 显示已创建的课程列表 | - |
| T1.4 | 编辑课程信息 | PUT /api/courses/{id} | 课程更新成功 | - |
| T1.5 | 删除课程 | DELETE /api/courses/{id} | 课程删除成功 | - |

### T2. 班级管理验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| T2.1 | 创建新班级 | POST /api/classes | 班级创建成功 | **#2 创建无法正确读取** |
| T2.2 | 查看班级列表 | GET /api/classes/course/{courseId} | 显示班级及学生数 | - |
| T2.3 | 添加学生 | POST /api/classes/{classId}/students/{studentId} | 学生添加成功 | **#2 添加功能无法添加学生** |
| T2.4 | 查看学生列表 | GET /api/classes/{classId}/students | 正确显示学生列表 | **#2 学生列表逻辑缺陷** |
| T2.5 | 移除学生 | DELETE /api/classes/{classId}/students/{studentId} | 学生移除成功 | - |

### T3. 题库管理验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| T3.1 | 创建题目（设公开） | POST /api/problems | 题目创建成功 | **#3 公开看不到** |
| T3.2 | 查看公开题库 | GET /api/problems/public | 公开题目可见 | **#3 公开看不到** |
| T3.3 | 添加测试用例 | POST /api/test-cases | 测试用例保存成功 | **#3 无法添加保存** |
| T3.4 | 查看测试用例 | GET /api/test-cases/problem/{id} | 显示所有测试用例 | - |

### T4. 公告管理验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| T4.1 | 创建公告 | POST /api/announcements | 公告创建成功 | - |
| T4.2 | 查看课程公告 | GET /api/announcements/course/{courseId} | 公告列表加载成功 | **#7 无法加载公告** |

---

## 学生端测试流程

### S1. 课程浏览验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| S1.1 | 登录学生账号 | POST /api/auth/login | 返回 JWT token | - |
| S1.2 | 查看可用课程 | GET /api/courses | 显示可加入的课程 | - |
| S1.3 | 通过邀请码加入 | POST /api/classes/join?inviteCode=XXX | 加入班级成功 | - |

### S2. 公告查看验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| S2.1 | 查看课程公告 | GET /api/announcements/course/{courseId} | 公告正常显示 | **#8 无法查看公告** |

### S3. 作业提交验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| S3.1 | 查看作业列表 | GET /api/assignments/course/{courseId} | 作业列表正常 | - |
| S3.2 | 查看作业详情 | GET /api/assignments/{id}/problems | 题目列表正常 | - |
| S3.3 | 提交代码 | POST /api/submissions | 提交成功，返回 submission | **#9 提交后系统繁忙** |
| S3.4 | 查询评分结果 | GET /api/submissions/{id}/grading | 返回评分状态和分数 | - |

---

## 管理端测试流程

### A1. 用户管理验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| A1.1 | 登录管理员 | POST /api/auth/login | 返回 JWT token | - |
| A1.2 | 创建新用户 | POST /api/admin/users | 用户创建成功 | **#10 无法创建新用户** |
| A1.3 | 查看用户列表 | GET /api/admin/users | 用户列表正常 | - |
| A1.4 | 编辑用户 | PUT /api/admin/users/{id} | 用户更新成功 | - |
| A1.5 | 删除用户 | DELETE /api/admin/users/{id} | 用户删除成功 | - |

### A2. 操作日志验证
| 步骤 | 操作 | API 端点 | 预期结果 | 关联缺陷 |
|------|------|----------|----------|----------|
| A2.1 | 查看操作日志 | GET /api/admin/logs | 日志列表正常显示 | **#11 无显示** |

---

## 前后端联调验证

### API 请求/响应格式一致性检查

对每个 API 端点，执行以下验证：

1. **请求格式验证**
   - 检查前端 API 调用 (`src/api/index.ts`) 中的 URL 模式与后端 `@RequestMapping` 匹配
   - 检查请求方法 (GET/POST/PUT/DELETE) 与后端 `@GetMapping/@PostMapping` 匹配
   - 检查请求参数名称和类型与后端 `@RequestParam/@PathVariable/@RequestBody` 匹配

2. **响应格式验证**
   - 后端统一返回 `Result<T> = { code, msg, data }`
   - 前端 `api/request.ts` 拦截器检查 `data.code !== 200` 做错误处理
   - 检查前端业务代码是否正确访问 `response.data.data` (经过拦截器解包后)

3. **Token 传递验证**
   - 前端 `api/request.ts` 拦截器自动添加 `Authorization: Bearer <token>`
   - 后端 `JwtAuthenticationFilter` 正确解析 Bearer token
   - 401 响应时前端自动跳转登录页

### 关键 URL 映射表

| 前端 API 调用 | HTTP Method | 后端 Controller 映射 |
|---------------|-------------|---------------------|
| `/auth/login` | POST | AuthController.login() |
| `/auth/register` | POST | AuthController.register() |
| `/courses` | GET/POST | CourseController |
| `/classes` | POST | ClassController.createClass() |
| `/classes/course/{id}` | GET | ClassController.getClassesByCourse() |
| `/classes/join` | POST | ClassController.joinClass() |
| `/problems/public` | GET | ProblemController.getPublicProblems() |
| `/submissions` | POST | SubmissionController.submitCode() |
| `/announcements/course/{id}` | GET | AnnouncementController.getAnnouncementsByCourse() |
| `/admin/users` | POST | AdminController.createUser() |
| `/admin/logs` | GET | AdminController.getRecentLogs() |

---

## 缺陷专项排查

### 排查策略

对每个已知缺陷，按以下顺序排查：

1. **前端请求检查**：浏览器 DevTools → Network 标签 → 检查请求 URL、参数、Headers
2. **后端日志检查**：查看 Spring Boot 控制台输出的 SQL 和异常日志
3. **数据库验证**：通过 MySQL 客户端检查数据是否正确持久化
4. **API 独立调用**：使用 Postman/curl 绕过前端直接调用后端 API

### 常见排查命令

```bash
# 检查 MySQL 数据
docker exec -it autograding-mysql mysql -uroot -p123456 autograding
SELECT * FROM course WHERE deleted=0;
SELECT * FROM class WHERE deleted=0;
SELECT * FROM problem WHERE is_public=1;

# 检查 Redis
docker exec -it autograding-redis redis-cli

# 检查后端日志
docker logs autograding-backend --tail 100

# 检查评分引擎日志
docker logs autograding-grading-worker --tail 100
```
