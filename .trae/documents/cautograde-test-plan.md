# CAutoGrade 测试计划

> 基于现有测试文档（defect-tracking.md、integration-test-summary.md、test-plan-integration.md）和当前代码状态制定

## 一、测试目标

验证系统在近期修复后的完整功能可用性，覆盖三个维度：
1. **前端测试**：验证 Vue 组件、路由守卫、状态管理、API 调用层
2. **后端测试**：验证 Service 层逻辑、Controller 接口、Security 鉴权
3. **组合测试**：端到端验证完整业务流程（含批阅引擎）

## 二、前置条件

### 2.1 环境准备
- Docker Compose 全栈启动（6 个容器正常运行）
- 确认后端 API 可达（`curl http://localhost:8080/api/auth/login`）
- 确认前端可访问（`http://localhost`）
- 确认 grading-worker 正常轮询（无 ERROR 日志）

### 2.2 测试账号
| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 教师 | teacher | 123456 |
| 学生 | student1 | 123456 |

### 2.3 近期修复回顾
- ✅ 502 问题修复（system_config 表 + UserRepository JPA 冲突）
- ✅ 批阅引擎 P1-P10 修复（docker SDK、重复编译、PROCESSING 状态、超时配置、supervisord 等）
- ✅ Issue 2-4 修复（配置持久化、评分复核逻辑、重复 avgScore 字段）
- ✅ operation_log.detail JSON→TEXT 修复（上次集成测试发现）
- ✅ announcement 表缺失修复（上次集成测试发现）

---

## 三、前端测试（Vitest）

### 3.1 运行现有测试套件
```bash
cd frontend && npm run test
```

**现有 7 个测试文件**：
- `api/index.test.ts` — API 请求拦截器
- `router/index.test.ts` — 路由守卫
- `composables/usePolling.test.ts` — 轮询 Hook
- `composables/usePermission.test.ts` — 权限检查
- `composables/useCountdown.test.ts` — 倒计时
- `stores/notification.test.ts` — 通知状态
- `stores/auth.test.ts` — 认证状态

**验证项**：
- [ ] 所有 7 个测试文件通过
- [ ] 如有失败，分析原因并修复

### 3.2 vite.config.ts 测试配置修复
当前 `vite.config.ts` 缺少 `test` 配置块，需添加：
```ts
test: {
  environment: 'jsdom',
  globals: true,
}
```

### 3.3 前端页面冒烟测试（手动）
通过浏览器逐页面验证：

**学生端**：
- [ ] 登录 → 学生仪表盘加载
- [ ] 我的课程列表显示
- [ ] 作业列表 → 作业详情 → 代码编辑器
- [ ] 提交代码 → 批改进度轮询 → 结果展示
- [ ] 提交历史查看

**教师端**：
- [ ] 登录 → 教师仪表盘加载
- [ ] 课程管理 CRUD
- [ ] 班级管理 + 学生导入
- [ ] 题库管理 + 测试用例
- [ ] 发布作业
- [ ] 批改审核
- [ ] 学情分析图表

**管理端**：
- [ ] 登录 → 系统概览
- [ ] 用户管理 CRUD
- [ ] 系统配置页面（含持久化验证）
- [ ] 操作日志

---

## 四、后端测试（JUnit 5 + Mockito）

### 4.1 运行现有测试套件
```bash
cd backend && mvn test
```

**现有 23 个测试文件**，重点验证：
- [ ] 全部测试通过
- [ ] 之前失败的 ClassControllerTest、SubmissionServiceTest 是否因 OperationLogService mock 问题修复

### 4.2 已知问题测试文件修复
上次集成测试发现 `ClassControllerTest` 和 `SubmissionServiceTest` 因未 mock `OperationLogService` 导致 NPE。需检查并修复。

### 4.3 新增测试（针对近期修复）

**SystemConfigService 测试**：
- [ ] 测试 `init()` 从数据库加载配置
- [ ] 测试 `init()` 数据库不可用时降级为默认值
- [ ] 测试 `updateConfig()` 正常写入
- [ ] 测试 `updateConfig()` 未知 key 抛异常

**AnalyticsService.getEffectiveScore 测试**：
- [ ] 测试 humanAdjustedScore 优先于 totalScore
- [ ] 测试 humanAdjustedScore 为 null 时使用 totalScore

**NotificationScheduler.checkStuckGradingTasks 测试**：
- [ ] 测试 PROCESSING 状态任务自动重置为 PENDING

---

## 五、组合测试（端到端）

### 5.1 完整业务链路测试

按以下顺序搭建完整数据链，验证端到端流程：

```
Step 1: 管理员创建教师账号
Step 2: 教师创建课程
Step 3: 教师创建班级 → 获取邀请码
Step 4: 学生通过邀请码加入班级
Step 5: 教师创建题目 + 测试用例
Step 6: 教师发布作业（关联题目到班级）
Step 7: 学生提交代码
Step 8: 等待批阅引擎处理（轮询 grading_status）
Step 9: 查看批阅结果（三维度评分 + 批注）
Step 10: 教师复核评分
Step 11: 查看学情分析数据
```

### 5.2 批阅引擎专项测试

**测试用例准备**：
创建一道简单的 C 语言题目，配置 2-3 个测试用例：
```c
// 题目：计算两数之和
// 输入：两个整数 a, b
// 输出：a + b 的值

#include <stdio.h>
int main() {
    int a, b;
    scanf("%d %d", &a, &b);
    printf("%d\n", a + b);
    return 0;
}
```

测试用例：
| 输入 | 期望输出 |
|------|----------|
| 1 2 | 3 |
| 10 20 | 30 |
| -1 1 | 0 |

**验证项**：
- [ ] 提交正确代码 → 编译通过 → 测试用例全部通过 → 评分 DONE
- [ ] 提交编译错误代码 → 编译失败 → 评分 DONE（低分）
- [ ] 提交部分通过代码 → 部分测试用例通过 → 评分 DONE
- [ ] grading_status 正确流转：PENDING → PROCESSING → DONE
- [ ] 批阅完成后学生收到通知

### 5.3 缺陷回归测试

对照 `defect-tracking.md` 中的 11 个已知缺陷，逐一验证：

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

### 5.4 配置持久化验证

- [ ] 修改 LLM 配置 → 重启后端容器 → 验证配置保留
- [ ] 修改沙箱配置 → 重启后端容器 → 验证配置保留
- [ ] 修改评分配置 → 重启后端容器 → 验证配置保留

### 5.5 安全性验证

- [ ] 未登录访问受保护 API → 返回 401
- [ ] 学生访问教师 API → 返回 403
- [ ] 教师访问管理员 API → 返回 403
- [ ] 过期 Token → 返回 401

---

## 六、执行步骤

### Phase 1：前端自动化测试
1. 修复 `vite.config.ts` 添加 test 配置
2. 运行 `npm run test`，记录结果
3. 修复失败的测试

### Phase 2：后端自动化测试
1. 运行 `mvn test`，记录结果
2. 修复 ClassControllerTest / SubmissionServiceTest 的 mock 问题
3. 新增 SystemConfigService / AnalyticsService 测试
4. 重新运行确认全部通过

### Phase 3：组合测试
1. 确认 Docker 全栈环境正常
2. 执行 5.1 完整业务链路测试
3. 执行 5.2 批阅引擎专项测试
4. 执行 5.3 缺陷回归测试
5. 执行 5.4 配置持久化验证
6. 执行 5.5 安全性验证

### Phase 4：结果汇总
1. 更新 defect-tracking.md 状态
2. 记录新发现的缺陷
3. 生成测试报告

---

## 七、风险与注意事项

1. **DEEPSEEK_API_KEY**：批阅引擎测试需要有效的 API Key，否则 LLM 层会失败（但会降级为规则评分）
2. **Docker Sandbox**：grading-worker 容器内需要能通过 docker.sock 操作宿主 Docker 来编译运行 C 代码
3. **测试数据隔离**：组合测试会写入真实数据库，需注意数据清理或使用专用测试数据库
4. **网络依赖**：LLM API 调用依赖外网，网络不稳定可能导致超时
