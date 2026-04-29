# CAutoGrade 组合测试总结与警示

> 测试执行时间：2026-04-30 06:27~06:35  
> 测试环境：Docker Compose 全栈（MySQL + Redis + ChromaDB + Backend + Worker + Nginx）

---

## 一、测试方法

按 `test-plan-integration.md` 中定义的教师端→学生端→管理端流程，以三种角色账号的真实 API 请求进行组合测试。同时运行后端 Maven 单元测试套件以验证自动化测试状态。

**测试账号实际使用情况**：

| 角色 | 文档标注 | 数据库实际 | 密码 |
|------|----------|-----------|------|
| 管理员 | admin | admin | admin123 |
| 教师 | teacher1 | **teacher** | 123456 |
| 学生 | student1 | student1 | 123456 |

---

## 二、缺陷发现与修复

### 致命缺陷（阻塞全部写入操作）

#### 根因：`operation_log.detail` 列类型错误

**现象**：教师无法创建课程/班级/题目/公告，管理员无法创建用户，全部返回 500 "系统繁忙"。

**定位**：后端日志显示 `DataIntegrityViolationException: Invalid JSON text in column 'operation_log.detail'`。

**根因**：`docker/mysql/init.sql` 中 `operation_log.detail` 定义为 `JSON` 类型，但业务代码在 `CourseService.createCourse()`、`ProblemService.createProblem()` 等方法中向该列插入普通中文字符串（如 `"创建课程: TestCourse1"`）。MySQL 严格模式拒绝非 JSON 数据，抛出异常并导致外层事务回滚。由于所有写入操作都在成功写入业务数据后调用 `operationLogService.logOperation()`，操日志写入失败会连带导致整个业务流程失败。

**修复**（3 处联动）：

| 位置 | 变更 |
|------|------|
| `OperationLog.java:48` | `@Column(columnDefinition = "JSON")` → `"TEXT"` |
| `docker/mysql/init.sql:209` | `detail JSON` → `detail TEXT` |
| MySQL 线上库 | `ALTER TABLE operation_log MODIFY COLUMN detail TEXT` |

**影响范围**：修复后课程 CRUD、班级管理（含添加/移除学生）、用户创建、操作日志全部恢复正常。

---

#### 致命缺陷：`announcement` 表缺失

**现象**：教师创建公告和查看公告均返回 500；日志显示 `Table 'autograding.announcement' doesn't exist`。

**根因**：`docker/mysql/init.sql` 中**完全没有** `announcement` 表的建表语句。`Announcement` 实体类和 `AnnouncementController`/`AnnouncementService` 都已编写完成，但数据库缺少对应表。这是首次 Docker 部署就会触发的生产级缺陷。

**修复**：在 `init.sql` 的通知表与操作日志表之间添加完整的 `announcement` 表 DDL，同时在线上库执行 `CREATE TABLE`。

---

### 中等问题

#### Problem 字段类型约定不明确

**现象**：创建题目时传入 `difficulty: "EASY"` 返回 500，后端报 `Cannot deserialize String "EASY" to Integer`。

**说明**：`Problem.difficulty` 字段定义为 `Integer`（1/2/3 对应 EASY/MEDIUM/HARD），`Problem.isPublic` 也是 `Integer`（0/1）。但 API 调用方可能传字符串或布尔值，导致 JSON 反序列化失败。

**建议**：在 Swagger/Knife4j 文档中明确标注字段类型，或前端侧统一做数值映射。

---

#### 测试用例隔离不足

**现象**：`ClassControllerTest` 和 `SubmissionServiceTest` 因未 mock `OperationLogService` 导致 NPE 失败。

**说明**：`OperationLogService` 被注入到 `CourseService`、`ProblemService`、`SubmissionService`、`ClassService` 等多个 service 中，但这些 service 的单元测试没有统一的 mock 基线。

**建议**：提供 `@TestConfiguration` 基类统一 mock `OperationLogService`，或将其调用改为可选（try-catch 包裹）。

---

## 三、测试覆盖矩阵

| 测试域 | 端点 | 修复前 | 修复后 |
|--------|------|--------|--------|
| 教师-课程 | POST /api/courses | ❌ | ✅ |
| 教师-课程 | GET /api/courses/teacher | ✅ | ✅ |
| 教师-课程 | PUT /api/courses/{id} | ❌ | ✅ |
| 教师-课程 | DELETE /api/courses/{id} | ❌ | ✅ |
| 教师-班级 | POST /api/classes | ❌ | ✅ |
| 教师-班级 | GET /api/classes/course/{id} | ❌ | ✅ |
| 教师-班级 | POST /api/classes/{id}/students/{sid} | ❌ | ✅ |
| 教师-班级 | GET /api/classes/{id}/students | ❌ | ✅ |
| 教师-班级 | DELETE /api/classes/{id}/students/{sid} | — | ✅ |
| 教师-题库 | POST /api/problems | ❌ | ✅ |
| 教师-题库 | GET /api/problems/public | ✅ | ✅ |
| 教师-题库 | POST /api/test-cases | ❌ | ✅ |
| 教师-题库 | GET /api/test-cases/problem/{id} | — | ✅ |
| 教师-公告 | POST /api/announcements | ❌ | ✅ |
| 教师-公告 | GET /api/announcements/course/{id} | ❌ | ✅ |
| 学生 | GET /api/courses | ✅ | ✅ |
| 学生 | GET /api/announcements/course/{id} | ❌ | ✅ |
| 管理 | POST /api/admin/users | ❌ | ✅ |
| 管理 | GET /api/admin/users | ✅ | ✅ |
| 管理 | GET /api/admin/logs | ✅ (空) | ✅ (有数据) |

---

## 四、架构警示

### 1. 操作日志不应阻塞核心业务

当前模式：业务方法 → 数据库写入 → 操作日志写入 → 返回结果。日志写入失败会导致整个业务事务回滚。

**建议改造**：

```java
// 当前（危险）
courseMapper.insert(course);                    // 成功
operationLogService.logOperation(...);           // 失败 → 异常 → 全部回滚

// 建议（安全）
courseMapper.insert(course);
try {
    operationLogService.logOperation(...);
} catch (Exception e) {
    log.warn("操作日志写入失败", e);  // 吞掉异常，不影响业务
}
```

或使用 `@Async` 异步写入 + 独立事务传播级别。

### 2. init.sql 必须与实体类保持同步

当前发现 `announcement` 表在实体类中已定义但 `init.sql` 中完全缺失。建议在 CI/CD 中增加 schema 一致性检查：扫描所有 `@Table/@TableName` 注解，与 `init.sql` 中的 `CREATE TABLE` 做差异比对。

### 3. MySQL JSON 列谨慎用于非 JSON 数据

MySQL 的 JSON 类型自带严格校验，只接受 `{"key":"value"}` 等合法 JSON。存储普通字符串应使用 `TEXT` 或 `VARCHAR`。`@Column(columnDefinition = "JSON")` 注解仅声明 DDL，不能改变运行时的数据校验行为。

### 4. 测试账号需与文档一致

测试文档 `test-plan-integration.md` 标注教师账号为 `teacher1`，但 `init.sql` 插入的是 `teacher`。部署后应第一时间验证文档与实际数据的一致性。

### 5. 前端请求字段类型必须与后端实体一致

`difficulty`（Integer vs String Enum）、`isPublic`（Integer vs Boolean）等字段的类型不一致会导致 JSON 反序列化异常。建议统一使用 DTO 而非裸实体接收请求。

---

## 五、下次测试建议

1. **搭建完整数据链**后再测提交：创建课程 → 班级 → 添加学生 → 创建作业 → 关联题目 → 学生提交 → 评分
2. 增加 **Celery/Redis 连通性**专项测试（`grading_queue` 消息投递验证）
3. 验证 **Docker Sandbox** 隔离执行（编译 + 运行无网络容器）
4. 补充 `SubmissionService` 和 `ClassController` 的单元测试 Mock 修复
