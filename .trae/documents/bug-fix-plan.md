# CAutoGrade BUG 修复计划

## 修复范围

基于对测试文件 `测试.txt` 中所有问题的代码级排查，以及对另一份诊断报告的逐条验证，确认以下需要修复的问题。

***

## 阶段一：消除系统性根因 — 清理 JPA 注解（修复 6+ 个 500 错误）

### 问题说明

所有 13 个实体类同时使用了 JPA 注解（`@Entity`、`@Table`、`@Column`、`@ManyToOne(fetch=LAZY)`、`@OneToOne(fetch=LAZY)`）和 MyBatis-Plus 注解（`@TableName`、`@TableId`、`@TableField`）。项目实际使用 MyBatis-Plus 作为 ORM，JPA 的 LAZY 代理在 JSON 序列化时触发 `LazyInitializationException`，被 `GlobalExceptionHandler` 兜底捕获返回 `{"code":500,"msg":"系统繁忙，请稍后重试"}`。

这是导致以下端点 500 的最可能系统性根因：

* `GET /api/gradings/pending`

* `GET /api/gradings/unreviewed`

* `GET /api/submissions/student`

* `GET /api/submissions/scores/assignment/{id}`

* `GET /api/analytics/class/{id}`

* `GET /api/admin/logs`

### 修改清单

对每个实体文件执行以下操作：

1. 删除 JPA 注解（`@Entity`、`@Table`、`@Column`、`@Enumerated`、`@GeneratedValue`、`@Id`、`@JoinColumn`、`@ManyToOne`、`@OneToOne`）
2. 删除 JPA LAZY 关联字段（标记了 `@TableField(exist=false)` + `@ManyToOne/@OneToOne` 的字段）
3. 删除对应的 `import jakarta.persistence.*` 语句
4. 保留 MyBatis-Plus 注解（`@TableName`、`@TableId`、`@TableField`、`@TableLogic`）
5. 保留 `@JsonIgnore` import（如果其他字段仍需要）

| #  | 文件                              | 需删除的 JPA LAZY 字段                                                       |
| -- | ------------------------------- | ---------------------------------------------------------------------- |
| 1  | `entity/User.java`              | 无 LAZY 字段，仅删除 JPA 类/字段注解                                               |
| 2  | `entity/Course.java`            | `teacher` 字段 (L49-53)                                                  |
| 3  | `entity/Class.java`             | `course` 字段 (L39-43)                                                   |
| 4  | `entity/ClassStudent.java`      | `classEntity` 字段 (L35-39), `student` 字段 (L44-48)                       |
| 5  | `entity/Assignment.java`        | `course` 字段 (L52-56), `creator` 字段 (L81-85)                            |
| 6  | `entity/AssignmentProblem.java` | `assignment` 字段 (L33-37), `problem` 字段 (L42-46)                        |
| 7  | `entity/Problem.java`           | `creator` 字段 (L63-67)                                                  |
| 8  | `entity/TestCase.java`          | `problem` 字段 (L36-40)                                                  |
| 9  | `entity/Submission.java`        | `assignment` 字段 (L36-40), `problem` 字段 (L45-49), `student` 字段 (L54-58) |
| 10 | `entity/GradingResult.java`     | `submission` 字段 (L43-47), `reviewer` 字段 (L80-84)                       |
| 11 | `entity/Notification.java`      | `user` 字段 (L41-45)                                                     |
| 12 | `entity/Announcement.java`      | `course` 字段 (L36-40), `publisher` 字段 (L45-49)                          |
| 13 | `entity/OperationLog.java`      | `user` 字段 (L35-39)                                                     |

### 修改原则

* `@Column(name = "xxx")` 中的列名映射信息，如果与 Java 字段名符合 MyBatis-Plus 驼峰转下划线规则则不需要额外注解；不符合的需补充 `@TableField("xxx")`

* `@Column` 中的 `nullable`、`unique`、`length`、`columnDefinition` 等约束信息删除（由数据库 DDL 管理）

* `@Enumerated(EnumType.STRING)` 删除，MyBatis-Plus 默认使用枚举名，需确认 `application.yml` 中 `mybatis-plus.configuration.default-enum-type-handler` 配置

***

## 阶段二：修复 Service 层查询缺陷

### 2.1 `selectOne` 多结果异常修复

**问题**：`selectOne` 在查询到多条记录时抛出 `TooManyResultsException`。

**修改清单**：

| # | 文件                       | 方法                                 | 行号       | 修复方式                  |
| - | ------------------------ | ---------------------------------- | -------- | --------------------- |
| 1 | `GradingService.java`    | `getGradingResultBySubmissionId()` | L99-103  | 添加 `.last("limit 1")` |
| 2 | `SubmissionService.java` | `getGradingResultBySubmission()`   | L90-94   | 添加 `.last("limit 1")` |
| 3 | `SubmissionService.java` | `updateGradingResult()`            | L105-107 | 添加 `.last("limit 1")` |
| 4 | `SubmissionService.java` | `updateGradingStatus()`            | L124-126 | 添加 `.last("limit 1")` |
| 5 | `AnalyticsService.java`  | `getAssignmentAnalytics()`         | L144-146 | 添加 `.last("limit 1")` |
| 6 | `AnalyticsService.java`  | `getProblemAnalytics()`            | L189-191 | 添加 `.last("limit 1")` |

### 2.2 `selectById(null)` 空指针防护

**问题**：当 ID 参数为 null 时，`selectById(null)` 可能抛出异常。

**修改清单**：

| # | 文件                          | 行号            | 修复方式                                               |
| - | --------------------------- | ------------- | -------------------------------------------------- |
| 1 | `GradingService.java`       | L79, L84, L89 | 在 `selectById` 前添加 null 检查                         |
| 2 | `SubmissionService.java`    | L184          | 在 `selectById` 前添加 null 检查                         |
| 3 | `SubmissionController.java` | L53           | `getCurrentUserId()` 返回 null 时抛出 BusinessException |

### 2.3 `updateById` 返回值未检查

**问题**：`SubmissionService.reviewGrading()` 中 `updateById` 返回值未检查，可能导致前端显示修改成功但数据库未更新。

**修改**：`SubmissionService.java` L147，检查返回值，若为 0 则抛出 BusinessException。

### 2.4 `Collectors.toMap` 重复 key 防护

**问题**：`AssignmentService.getProblemDetails()` L154-155 使用 `Collectors.toMap` 无 merge 函数，重复 key 时抛 `IllegalStateException`。

**修改**：改为 `Collectors.toMap(Problem::getId, p -> p, (a, b) -> a)`。

***

## 阶段三：修复 `Result` 类和异常处理

### 3.1 `Result.error()` 支持自定义 code

**问题**：`Result.java` 的 `error()` 方法硬编码 `code=500`，所有错误（包括业务异常和权限异常）都返回 500，前端无法区分。

**修改**：`Result.java` 添加重载方法：

```java
public static <T> Result<T> error(int code, String msg) {
    Result<T> m = new Result<>();
    m.setCode(code);
    m.setMsg(msg);
    m.setData(null);
    return m;
}
```

### 3.2 `GlobalExceptionHandler` 区分异常类型

**问题**：`BusinessException` 也返回 code=500，应返回业务错误码。

**修改**：`GlobalExceptionHandler.java` L31-34，改为 `Result.error(e.getCode(), e.getMessage())`。

需要同步修改 `BusinessException` 类，添加 `code` 字段（默认 400）。

### 3.3 `AccessDeniedException` 返回 403

**问题**：当前 `handleAccessDeniedException` 返回 `Result.error("无权限访问该资源")`，code 仍为 500。

**修改**：改为 `Result.error(403, "无权限访问该资源")`。

***

## 阶段四：修复学情分析前后端字段不匹配

**问题**：前端 `Analytics.vue` 期望字段 `avgScore`、`passRate`、`excellentRate`、`scoreDistribution`、`knowledgePoints`、`errorTop10`，但后端 `AnalyticsService.getClassAnalytics()` 只返回 `totalStudents`、`submissionRate`、`averageScore`。

**修改**：`AnalyticsService.java` L38-92，补充缺失字段的计算逻辑：

* `avgScore` → 使用已有的 `averageScore`

* `passRate` → 统计 totalScore >= 60 的比例

* `excellentRate` → 统计 totalScore >= 90 的比例

* `scoreDistribution` → 按分数段（0-59, 60-69, 70-79, 80-89, 90-100）统计人数

* `knowledgePoints` → 暂返回空列表（需关联 Problem 的 knowledgeTags）

* `errorTop10` → 暂返回空列表（需更复杂的数据聚合）

***

## 阶段五：修复前端问题

### 5.1 AdminClasses 补充"添加班级"按钮和对话框

**问题**：`AdminClasses.vue` 脚本中有 `handleCreate`、`dialogVisible`、`createForm` 逻辑，但模板中缺少按钮和对话框。

**修改**：在 `AdminClasses.vue` 模板中添加：

1. "创建班级"按钮（在 page-header 中）
2. `<el-dialog>` 创建班级对话框（包含班级名称输入框）

### 5.2 公开题库不显示问题排查

**问题**：API 返回正常但前端不渲染。代码逻辑 `publicProblems.value` 解包正确，需进一步排查。

**排查方向**：

1. 检查 `loadPublicProblems` 和 `loadProblems` 并发执行时 `loading` 状态是否互相覆盖
2. 检查浏览器 DevTools Network 面板确认请求是否成功
3. 检查浏览器控制台是否有 JavaScript 错误

**可能的修复**：为 `loadPublicProblems` 使用独立的 `publicLoading` 状态变量，避免与 `loadProblems` 的 `loading` 冲突。

***

## 阶段六：补充 `SecurityUtils` 防御性编程

**问题**：`SecurityUtils.getCurrentUserId()` 可能返回 null，调用方未做 null 检查。

**修改**：

1. `SecurityUtils.getCurrentUserId()` 添加 null 时抛出 `BusinessException("用户未登录")`
2. 或者在调用方（`SubmissionController`、`GradingController` 等）添加 null 检查

***

## 执行顺序

1. **阶段一**（最高优先级）— 清理 JPA 注解，一次性修复 6+ 个 500 错误
2. **阶段三.1 + 三.2 + 三.3** — 修复 Result 类和异常处理（为后续调试提供更好的错误信息）
3. **阶段二.1 + 二.2** — 修复 selectOne 和 selectById 缺陷
4. **阶段四** — 修复学情分析字段不匹配
5. **阶段五.1** — 补充 AdminClasses UI
6. **阶段二.3 + 二.4** — 修复 updateById 返回值和 toMap 重复 key
7. **阶段五.2** — 排查公开题库不显示
8. **阶段六** — SecurityUtils 防御性编程

***

## 验证方式

每个阶段修复后：

1. 运行后端单元测试：`cd backend && mvn test`
2. 运行前端构建：`cd frontend && npm run build`
3. 使用测试文件 `测试.txt` 中的端点逐一验证
4. 检查后端日志确认无异常：`docker logs autograding-backend --tail 100`

