# C语言自动批阅系统 - 启动检查清单

## 环境要求
- [x] Docker Desktop 已安装并运行
- [x] Node.js 24.x 已安装
- [x] npm 11.x 已安装

## 已修复的问题
- [x] 前端 Vite proxy 配置错误（rewrite 规则导致路径错误）
- [x] 后端 CORS 配置错误（`allowCredentials` 与 `*` origin 冲突）
- [x] GradingResult 实体缺少 `@TableField(exist = false)` 注解
- [x] nginx 缺少前端文件挂载（已在 docker-compose.yml 中添加）

## 启动步骤
1. [x] 启动 Docker Desktop
2. [x] 运行 `start.bat` 或执行 `docker-compose up -d`
3. [x] 访问前端 http://localhost:3000
4. [x] 注册或登录系统

## 服务状态检查
- [x] MySQL: localhost:3306
- [x] Redis: localhost:6379
- [x] Backend: http://localhost:8080
- [x] Frontend: http://localhost:3000

## 默认账号（可能需要重新注册）
- admin / admin123
- teacher1 / admin123
- student1 / admin123

## API 测试
- [x] POST /api/auth/login - 登录
- [x] POST /api/auth/register - 注册
- [x] GET /api/classes/student - 获取学生班级列表

## 注意事项
1. 如果默认账号登录失败，请使用注册功能创建新账号
2. 首次启动需要等待约15秒让所有服务就绪
3. 需要配置 DEEPSEEK_API_KEY 环境变量才能使用自动批阅功能