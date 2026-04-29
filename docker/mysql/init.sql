-- 初始化数据库结构
CREATE DATABASE IF NOT EXISTS autograding DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE autograding;

-- ==========================================================
-- 用户表 (User)
-- ==========================================================
CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/学号/工号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT '加密密码',
  `nickname` VARCHAR(50) COMMENT '昵称/真实姓名',
  `avatar` VARCHAR(255) COMMENT '头像URL',
  `role` ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '角色',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态 1：正常，0：禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==========================================================
-- 课程表 (Course)
-- ==========================================================
CREATE TABLE `course` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `description` TEXT COMMENT '课程描述',
  `teacher_id` BIGINT NOT NULL COMMENT '授课教师ID',
  `semester` VARCHAR(20) COMMENT '学期，例如 2026-Spring',
  `cover_url` VARCHAR(255) COMMENT '课程封面图URL',
  `invite_code` VARCHAR(20) UNIQUE COMMENT '课程邀请码',
  `status` ENUM('ACTIVE', 'ARCHIVED') DEFAULT 'ACTIVE' COMMENT '课程状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0,
  INDEX `idx_teacher_id` (`teacher_id`),
  CONSTRAINT `fk_course_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ==========================================================
-- 班级表 (Class)
-- ==========================================================
CREATE TABLE `class` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL COMMENT '班级名称（如：软工1班）',
  `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
  `invite_code` VARCHAR(20) UNIQUE NOT NULL COMMENT '选课邀请码',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0,
  INDEX `idx_course_id` (`course_id`),
  CONSTRAINT `fk_class_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ==========================================================
-- 班级-学生关联表 (Class_Student)
-- ==========================================================
CREATE TABLE `class_student` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
  CONSTRAINT `fk_cs_class` FOREIGN KEY (`class_id`) REFERENCES `class` (`id`),
  CONSTRAINT `fk_cs_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关联表';

-- ==========================================================
-- 作业表 (Assignment)
-- ==========================================================
CREATE TABLE `assignment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(100) NOT NULL COMMENT '作业标题',
  `description` TEXT COMMENT '作业详细说明',
  `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '截止时间',
  `max_score` INT DEFAULT 100 COMMENT '满分分值',
  `type` ENUM('EXAM', 'LAB', 'PRACTICE') DEFAULT 'PRACTICE' COMMENT '作业类型',
  `status` ENUM('DRAFT', 'PUBLISHED', 'EXPIRED', 'ARCHIVED') DEFAULT 'DRAFT' COMMENT '作业状态',
  `late_penalty_score` INT DEFAULT 0 COMMENT '逾期扣分',
  `created_by` BIGINT NOT NULL COMMENT '创建教师ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0,
  INDEX `idx_course` (`course_id`),
  CONSTRAINT `fk_assignment_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- ==========================================================
-- 题库表 (Problem)
-- ==========================================================
CREATE TABLE `problem` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(100) NOT NULL COMMENT '题目标题',
  `description` TEXT NOT NULL COMMENT '题目正文',
  `difficulty` TINYINT DEFAULT 1 COMMENT '难度等级 (1-5星)',
  `input_desc` TEXT COMMENT '输入说明',
  `output_desc` TEXT COMMENT '输出说明',
  `time_limit_ms` INT DEFAULT 1000 COMMENT '时间限制(ms)',
  `memory_limit_kb` INT DEFAULT 256000 COMMENT '内存限制(KB)',
  `knowledge_tags` JSON COMMENT '知识点标签',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `is_public` TINYINT(1) DEFAULT 0 COMMENT '是否公开至公共题库',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- ==========================================================
-- 作业-题目关联表 (Assignment_Problem)
-- ==========================================================
CREATE TABLE `assignment_problem` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `assignment_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `sort_order` INT DEFAULT 0 COMMENT '题目顺序',
  UNIQUE KEY `uk_assignment_problem` (`assignment_id`, `problem_id`),
  CONSTRAINT `fk_ap_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`id`),
  CONSTRAINT `fk_ap_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业与题目多对多包含关系';

-- ==========================================================
-- 测试用例表 (Test_Case)
-- ==========================================================
CREATE TABLE `test_case` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `problem_id` BIGINT NOT NULL,
  `input_data` TEXT COMMENT '输入数据',
  `expected_output` TEXT COMMENT '期望输出',
  `is_hidden` TINYINT(1) DEFAULT 1 COMMENT '是否对学生隐藏',
  `weight` INT DEFAULT 10 COMMENT '分数权重(百分比或绝对分)',
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0,
  INDEX `idx_problem` (`problem_id`),
  CONSTRAINT `fk_test_case_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目测试用例';

-- ==========================================================
-- 提交记录表 (Submission)
-- ==========================================================
CREATE TABLE `submission` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `assignment_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `code_content` LONGTEXT NOT NULL COMMENT 'C语言代码内容',
  `language` VARCHAR(20) DEFAULT 'c',
  `submit_count` INT DEFAULT 1 COMMENT '本题第几次提交',
  `is_late` TINYINT(1) DEFAULT 0 COMMENT '是否为迟交',
  `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) DEFAULT 0,
  INDEX `idx_student` (`student_id`),
  CONSTRAINT `fk_sub_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`id`),
  CONSTRAINT `fk_sub_problem` FOREIGN KEY (`problem_id`) REFERENCES `problem` (`id`),
  CONSTRAINT `fk_sub_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交记录表';

-- ==========================================================
-- 批改结果表 (Grading_Result)
-- ==========================================================
CREATE TABLE `grading_result` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `submission_id` BIGINT NOT NULL UNIQUE COMMENT '一对一关联提交易',
  `total_score` DECIMAL(5,2) COMMENT '总得分',
  `correctness_score` DECIMAL(5,2) COMMENT '正确性得分',
  `style_score` DECIMAL(5,2) COMMENT '规范性得分',
  `efficiency_score` DECIMAL(5,2) COMMENT '效率得分',
  `feedback_json` JSON COMMENT '批注与建议列表',
  `test_case_result` JSON COMMENT '各测试用例通过情况',
  `static_analysis_result` JSON COMMENT '静态分析结果(内存泄漏/语法)',
  `llm_raw_response` TEXT COMMENT '大模型原始回复',
  `grading_status` ENUM('PENDING', 'PROCESSING', 'DONE', 'FAILED') DEFAULT 'PENDING' COMMENT '批阅状态',
  `reviewed_by` BIGINT COMMENT '负责审核复核的教师ID',
  `human_adjusted_score` DECIMAL(5,2) COMMENT '人工复核后的修正分数',
  `reviewed_at` DATETIME COMMENT '教师复核时间',
  `graded_at` DATETIME COMMENT '自动批阅完成时间',
  CONSTRAINT `fk_gr_submission` FOREIGN KEY (`submission_id`) REFERENCES `submission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI代码批阅结果';

-- ==========================================================
-- 通知消息表 (Notification)
-- ==========================================================
CREATE TABLE `notification` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL COMMENT '接收者',
  `title` VARCHAR(100) NOT NULL,
  `content` TEXT,
  `type` ENUM('ASSIGNMENT', 'GRADING', 'SYSTEM', 'ANNOUNCEMENT') NOT NULL,
  `is_read` TINYINT(1) DEFAULT 0,
  `related_id` BIGINT COMMENT '相关业务ID(如作业ID)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  CONSTRAINT `fk_notice_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- ==========================================================
-- 插入系统默认初始数据
-- ==========================================================
-- 插入默认用户 (密码均为 123456)
INSERT INTO `user` (`username`, `password_hash`, `nickname`, `role`) VALUES
  ('admin',   '$2a$10$rksYpyhDhQWYv0hbD.fsNOOhynKOw/4TBFHb/NSgZsotML4DuRKdy', '系统管理员', 'ADMIN'),
  ('teacher', '$2a$10$rksYpyhDhQWYv0hbD.fsNOOhynKOw/4TBFHb/NSgZsotML4DuRKdy', '张老师',     'TEACHER'),
  ('student', '$2a$10$rksYpyhDhQWYv0hbD.fsNOOhynKOw/4TBFHb/NSgZsotML4DuRKdy', '小明',       'STUDENT');
