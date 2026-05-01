# CAutoGrade 测试执行计划

> 基于 `docs/defect-tracking.md`、`docs/test-plan-integration.md`、`docs/integration-test-summary.md` 及当前代码状态制定

## 当前状态

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1: 前端自动化测试 | ✅ 已完成 | 7 文件 85 测试全部通过 |
| Phase 2: 后端自动化测试 | ✅ 已完成 | 24 文件 175 测试全部通过 + 评分引擎 23 测试通过 |
| Phase 3: 组合测试 | ✅ 已完成 | 业务链路 8/11 通过，缺陷回归 10/11 通过，配置持久化✅，安全性✅ |
| Phase 4: 结果汇总 | ✅ 已完成 | 缺陷追踪表已更新 |

---

## Phase 2：后端自动化测试（继续）

### 2.1 验证现有测试修复结果

运行 `mvn test` 检查第5轮修复后的测试结果。之前的修复包括：
- 5个 Controller 测试添加了 `excludeFilters` 排除 `SecurityConfig`
- `AdminControllerTest` 添加了 `@MockBean SystemConfigService`
- 3个 Controller 测试使用 `mockStatic(SecurityUtils.class)` + `requireCurrentUserId()`
- `CourseControllerTest` 添加了 `userMapper.selectById(1L)` mock
- 3个 Service 测试添加了 `lenient()` 包装

**预期**：163 个测试全部通过，0 失败 0 错误

**如仍有失败**：逐个分析失败原因并修复，重点关注：
- `SubmissionControllerTest` 的 mock 链完整性
- `CourseControllerTest.getAllCourses` 的 userMapper mock
- `ClassControllerTest` 的 SecurityUtils mock 生命周期

### 2.2 新增测试：SystemConfigServiceTest

创建 `backend/src/test/java/com/autograding/service/SystemConfigServiceTest.java`

测试用例：
1. `testInit_loadsFromDatabase` — 数据库有配置时，init() 加载数据库值
2. `testInit_dbUnavailable_usesDefaults` — 数据库不可用时，init() 降级为默认值
3. `testGetAllConfig_returnsAllKeys` — getAllConfig() 返回 llm/sandbox/scoring 三个 key
4. `testUpdateConfig_persistsToDb` — updateConfig() 更新缓存并写入数据库
5. `testUpdateConfig_unknownKey_throwsException` — updateConfig("unknown", ...) 抛出 IllegalArgumentException
6. `testUpdateConfig_dbUnavailable_onlyUpdatesCache` — dbAvailable=false 时只更新缓存不写库

Mock 依赖：`SystemConfigMapper`、`ObjectMapper`

### 2.3 新增测试：AnalyticsService getEffectiveScore 逻辑

在现有 `AnalyticsServiceTest.java` 中追加测试用例：

1. `testGetEffectiveScore_prefersHumanAdjusted` — humanAdjustedScore 非 null 时使用它
2. `testGetEffectiveScore_fallsBackToTotalScore` — humanAdjustedScore 为 null 时使用 totalScore
3. `testGetEffectiveScore_bothNull_returnsNull` — 两者都为 null 时返回 null

### 2.4 新增测试：NotificationScheduler checkStuckGradingTasks

创建 `backend/src/test/java/com/autograding/scheduler/NotificationSchedulerTest.java`

测试用例：
1. `testCheckStuckGradingTasks_resetsProcessingToPending` — PROCESSING 状态重置为 PENDING
2. `testCheckStuckGradingTasks_logsStuckPendingCount` — 记录卡住的 PENDING 任务数
3. `testCheckStuckGradingTasks_noStuckTasks_noAction` — 无卡住任务时不做操作

Mock 依赖：`GradingResultMapper`、`AssignmentMapper`、`ClassMapper`、`ClassStudentMapper`、`SubmissionMapper`、`NotificationService`、`RedisTemplate`

### 2.5 运行评分引擎测试

```bash
cd grading-engine && python -m pytest tests/ -v
```

4 个测试文件：`test_scorer.py`、`test_retry.py`、`test_result_formatter.py`、`test_code_parser.py`

如有失败则修复。

---

## Phase 3：组合测试（端到端）

### 3.1 环境准备

1. 确认 Docker Compose 全栈运行（6 容器）
2. 确认后端 API 可达：`curl http://localhost:8080/api/auth/login`
3. 确认前端可访问：`http://localhost`
4. 确认 grading-worker 正常轮询：`docker logs autograding-grading-worker --tail 20`

### 3.2 完整业务链路测试（11 步）

按顺序执行，每步验证 API 返回：

| Step | 操作 | API | 验证点 |
|------|------|-----|--------|
| 1 | 管理员创建教师账号 | POST /api/admin/users | 返回 code=200 |
| 2 | 教师创建课程 | POST /api/courses | 返回课程信息 |
| 3 | 教师创建班级 | POST /api/classes | 返回含 inviteCode |
| 4 | 学生加入班级 | POST /api/classes/join | 加入成功 |
| 5 | 教师创建题目 | POST /api/problems | 返回题目信息 |
| 6 | 教师添加测试用例 | POST /api/test-cases | 返回用例信息 |
| 7 | 教师发布作业 | POST /api/assignments | 返回作业信息 |
| 8 | 学生提交代码 | POST /api/submissions | 返回 submission，status=PENDING |
| 9 | 等待批阅引擎处理 | GET /api/submissions/{id}/grading | 轮询至 status=DONE |
| 10 | 教师复核评分 | PUT /api/submissions/grading/{id}/review | humanAdjustedScore 生效 |
| 11 | 查看学情分析 | GET /api/analytics/class/{id} | averageScore 反映调整后分数 |

### 3.3 批阅引擎专项测试

创建 C 语言题目"计算两数之和"，配置 3 个测试用例：

| 输入 | 期望输出 |
|------|----------|
| 1 2 | 3 |
| 10 20 | 30 |
| -1 1 | 0 |

验证项：
- [ ] 正确代码 → 编译通过 → 全部通过 → DONE
- [ ] 编译错误代码 → 编译失败 → DONE（低分）
- [ ] 部分通过代码 → 部分通过 → DONE
- [ ] grading_status 流转：PENDING → PROCESSING → DONE
- [ ] 批阅完成后学生收到通知

### 3.4 缺陷回归测试

对照 `defect-tracking.md` 11 个已知缺陷逐一验证：

| # | 缺陷 | 验证方式 | 预期 |
|---|------|----------|------|
| 1 | 教师无法创建课程 | POST /api/courses | ✅ 创建成功 |
| 2 | 班级管理异常 | POST /api/classes + 添加学生 | ✅ 全部正常 |
| 3 | 公开题目不可见 | GET /api/problems/public | ✅ 可见 |
| 4 | 批阅审核无法测试 | 完成提交→评分→审核流程 | ✅ 可测试 |
| 5 | 成绩导出无法测试 | 完成评分后导出 | ✅ 可测试 |
| 6 | 学情分析无法测试 | 完成评分后查看 | ✅ 可测试 |
| 7 | 公告无法加载 | GET /api/announcements/course/{id} | ✅ 正常加载 |
| 8 | 学生无法查看公告 | 学生端查看课程公告 | ✅ 正常显示 |
| 9 | 提交后系统繁忙 | POST /api/submissions | ✅ 提交成功 |
| 10 | 无法创建新用户 | POST /api/admin/users | ✅ 创建成功 |
| 11 | 操作日志无显示 | GET /api/admin/logs | ✅ 有数据 |

### 3.5 配置持久化验证

1. 修改 LLM 配置 → 重启后端容器 → 验证配置保留
2. 修改沙箱配置 → 重启后端容器 → 验证配置保留
3. 修改评分配置 → 重启后端容器 → 验证配置保留

### 3.6 安全性验证

1. 未登录访问受保护 API → 返回 401
2. 学生访问教师 API → 返回 403
3. 教师访问管理员 API → 返回 403
4. 过期 Token → 返回 401

---

## Phase 4：结果汇总

1. 更新 `docs/defect-tracking.md` 中各缺陷状态（待修复 → 已修复/已验证）
2. 记录新发现的缺陷
3. 生成测试报告，包含：
   - 前端测试结果（85 测试通过）
   - 后端测试结果（含新增测试）
   - 评分引擎测试结果
   - 组合测试结果（业务链路 + 缺陷回归）
   - 配置持久化验证结果
   - 安全性验证结果

---

## 执行顺序

1. **Phase 2.1** — 运行 `mvn test`，验证现有测试修复结果
2. **Phase 2.2** — 创建 `SystemConfigServiceTest.java`
3. **Phase 2.3** — 扩展 `AnalyticsServiceTest.java`
4. **Phase 2.4** — 创建 `NotificationSchedulerTest.java`
5. **Phase 2.5** — 运行评分引擎 pytest
6. **Phase 2.6** — 再次运行 `mvn test` 确认全部通过
7. **Phase 3.1** — 环境准备检查
8. **Phase 3.2** — 完整业务链路测试
9. **Phase 3.3** — 批阅引擎专项测试
10. **Phase 3.4** — 缺陷回归测试
11. **Phase 3.5** — 配置持久化验证
12. **Phase 3.6** — 安全性验证
13. **Phase 4** — 更新缺陷追踪表 + 生成测试报告
