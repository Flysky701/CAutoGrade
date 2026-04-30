# CAutoGrade 缺陷追踪表

> 最后更新：2026-04-30 | 基于 测试.txt 记录的 11 个已知缺陷

| # | 模块 | 缺陷描述 | 严重程度 | 排查策略 | 涉及 API | 关联测试 | 修复方向 | 状态 |
|---|------|----------|----------|----------|----------|----------|----------|------|
| 1 | 教师-课程管理 | 无法创建新课程（创建无法正确读取） | 🔴 高 | 检查 POST /api/courses 请求体格式；检查 SecurityUtils.getCurrentUserId() 是否返回有效 ID；检查数据库 course 表是否写入 | POST /api/courses | CourseControllerTest.createCourse_shouldReturnCourse | 检查 CourseController 中 teacherId 获取逻辑；验证 CourseService.createCourse 的 UserMapper 查询 | 🔴 待修复 |
| 2 | 教师-班级管理 | 无法添加新班级；无法添加学生；学生列表逻辑缺陷 | 🔴 高 | 检查 POST /api/classes 请求；检查 POST /api/classes/{classId}/students/{studentId}；检查 GET /api/classes/{classId}/students 返回数据 | POST /api/classes, POST /api/classes/{classId}/students/{studentId} | ClassControllerTest.createClass, addStudent, getClassStudents | 检查班级创建时的 courseId 关联；检查学生添加的去重逻辑；检查学生列表的 User 关联查询 | 🔴 待修复 |
| 3 | 教师-题库管理 | 公开题目在公开题库不可见；测试用例无法正常添加保存 | 🔴 高 | 检查 is_public 字段是否在创建时正确设置；检查 GET /api/problems/public 查询条件；检查测试用例表数据 | POST /api/problems, GET /api/problems/public, POST /api/test-cases | ProblemServiceTest, TestCaseServiceTest | 检查 Problem 实体 isPublic 字段映射；检查 TestCase 关联 problemId | 🔴 待修复 |
| 4 | 教师-批阅审核 | 功能无法测试 | 🟡 中 | 需要先完成作业提交→评分流程，再验证 GET /api/gradings/pending 和 PUT /api/submissions/grading/{id}/review | GET /api/gradings/pending, PUT /api/submissions/grading/{id}/review | GradingServiceTest, SubmissionControllerTest.reviewGrading | 确认评分流程完整后验证批阅功能 | 🟡 待验证 |
| 5 | 教师-成绩导出 | 功能无法测试 | 🟡 中 | 需要先完成评分流程，再验证成绩导出 API；检查 FileService.storeFile 的 Excel 生成逻辑 | 成绩导出 API | FileServiceTest | FileService 文件生成逻辑已验证通过 | 🟡 待验证 |
| 6 | 教师-学情分析 | 功能无法测试 | 🟡 中 | 检查 GET /api/analytics/class/{id} 等接口返回数据格式；验证数据聚合逻辑 | GET /api/analytics/* | AnalyticsServiceTest (8 tests pass) | AnalyticsService 测试全部通过，前端渲染可能有问题 | 🟡 排查中 |
| 7 | 教师-公告管理 | 显示系统繁忙/无法加载公告 | 🔴 高 | 检查 GET /api/announcements/course/{courseId} 返回状态；检查 AnnouncementService.createAnnouncement 的权限校验 | GET /api/announcements/course/{courseId}, POST /api/announcements | AnnouncementServiceTest | 检查前端请求参数 courseId 是否正确传递；检查 AnnouncementController 路径映射 | 🔴 待修复 |
| 8 | 学生-主页 | 主页无法查看老师发布的公告 | 🔴 高 | 学生端调用 GET /api/announcements/course/{courseId} 时 courseId 是否为学生已加入课程的 ID；检查 ClassStudent 关联 | GET /api/announcements/course/{courseId} | - | 学生 Dashboard 需要先获取已加入课程的 courseId，再查公告 | 🔴 待修复 |
| 9 | 学生-我的课程 | 无法正常提交作业，提交后显示系统繁忙 | 🔴 高 | 检查 POST /api/submissions 请求格式；检查 submissionService.submitCode 的 assignmentMapper.selectById；检查评分引擎 Redis 连接 | POST /api/submissions | SubmissionControllerTest, SubmissionServiceTest | 检查 Celery/Redis 连通性；检查评分任务是否正确投递到 grading_queue；检查 Docker sandbox 是否可用 | 🔴 待修复 |
| 10 | 管理端-用户管理 | 无法创建新用户 | 🔴 高 | 检查 POST /api/admin/users 请求格式；检查 AdminController 的 @PreAuthorize 权限注解；检查 userService.createUserByAdmin 方法 | POST /api/admin/users | AdminControllerTest.createUser_shouldSucceed | 检查 User 实体 JSON 序列化；检查密码编码逻辑 | 🔴 待修复 |
| 11 | 管理端-操作日志 | 无显示 | 🟡 中 | 检查 GET /api/admin/logs 返回数据；检查 OperationLogService.getRecentLogs 查询条件；检查操作日志是否在业务操作时被写入 | GET /api/admin/logs | OperationLogServiceTest (5 tests pass), AdminControllerTest.getRecentLogs | OperationLogService 测试通过；需要在业务层添加 logOperation 调用 | 🟡 排查中 |

---
*严重程度：🔴 高 = 核心功能阻塞 | 🟡 中 = 功能受限 | 🟢 低 = 体验问题*
