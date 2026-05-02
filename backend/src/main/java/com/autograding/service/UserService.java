package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.ClassStudent;
import com.autograding.entity.Course;
import com.autograding.entity.User;
import com.autograding.mapper.ClassMapper;
import com.autograding.mapper.ClassStudentMapper;
import com.autograding.mapper.CourseMapper;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final ClassStudentMapper classStudentMapper;
    private final CourseMapper courseMapper;
    private final ClassMapper classMapper;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, OperationLogService operationLogService,
                       ClassStudentMapper classStudentMapper, CourseMapper courseMapper, ClassMapper classMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
        this.classStudentMapper = classStudentMapper;
        this.courseMapper = courseMapper;
        this.classMapper = classMapper;
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
               .eq(User::getDeleted, 0);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public List<User> getAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .orderByAsc(User::getRole)
               .orderByDesc(User::getCreatedAt);
        return userMapper.selectList(wrapper);
    }

    public List<User> getUsersByRole(User.Role role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role)
               .eq(User::getDeleted, 0)
               .orderByDesc(User::getCreatedAt);
        return userMapper.selectList(wrapper);
    }

    public User updateProfile(Long userId, String nickname, String avatar, String code) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }

        // 普通用户（非管理员）不允许自行修改学号/工号
        if (code != null && !code.equals(user.getCode()) && user.getRole() != User.Role.ADMIN) {
            throw new BusinessException("无权修改学号/工号");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(nickname != null, User::getNickname, nickname)
                .set(avatar != null, User::getAvatar, avatar)
                .set(code != null, User::getCode, code)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
        return getUserById(userId);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    // ==================== 管理员专用 ====================

    public User createUserByAdmin(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())
                .eq(User::getDeleted, 0)
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        if (user.getCode() != null && !user.getCode().isBlank()) {
            User codeExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getCode, user.getCode())
                    .eq(User::getDeleted, 0)
                    .last("limit 1"));
            if (codeExists != null) {
                throw new BusinessException("学号/工号已存在");
            }
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash() != null ? user.getPasswordHash() : "123456"));
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "CREATE_USER", "USER", user.getId(),
                "管理员创建用户: " + user.getUsername(), null);
        return user;
    }

    public User updateUserByAdmin(Long userId, User request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }

        // 校验用户名唯一
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, request.getUsername())
                    .eq(User::getDeleted, 0)
                    .last("limit 1"));
            if (exists != null) {
                throw new BusinessException("用户名已存在");
            }
        }

        // 校验学号/工号唯一
        if (request.getCode() != null && !request.getCode().equals(user.getCode())) {
            User codeExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getCode, request.getCode())
                    .eq(User::getDeleted, 0)
                    .last("limit 1"));
            if (codeExists != null) {
                throw new BusinessException("学号/工号已存在");
            }
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(request.getUsername() != null, User::getUsername, request.getUsername())
                .set(request.getCode() != null, User::getCode, request.getCode())
                .set(request.getNickname() != null, User::getNickname, request.getNickname())
                .set(request.getAvatar() != null, User::getAvatar, request.getAvatar())
                .set(request.getRole() != null, User::getRole, request.getRole())
                .set(request.getStatus() != null, User::getStatus, request.getStatus())
                .set(User::getUpdatedAt, LocalDateTime.now());

        if (request.getPasswordHash() != null && !request.getPasswordHash().isBlank()) {
            wrapper.set(User::getPasswordHash, passwordEncoder.encode(request.getPasswordHash()));
        }

        userMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "UPDATE_USER", "USER", userId,
                "管理员更新用户信息", null);
        return getUserById(userId);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() == User.Role.ADMIN) {
            throw new BusinessException("不能删除管理员账号");
        }

        if (user.getRole() == User.Role.STUDENT) {
            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getStudentId, userId);
            classStudentMapper.delete(csWrapper);
        }

        if (user.getRole() == User.Role.TEACHER) {
            LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
            courseWrapper.eq(Course::getTeacherId, userId)
                         .eq(Course::getDeleted, 0);
            List<Course> teacherCourses = courseMapper.selectList(courseWrapper);
            for (Course course : teacherCourses) {
                LambdaQueryWrapper<com.autograding.entity.Class> classWrapper = new LambdaQueryWrapper<>();
                classWrapper.eq(com.autograding.entity.Class::getCourseId, course.getId())
                            .eq(com.autograding.entity.Class::getDeleted, 0);
                List<com.autograding.entity.Class> classes = classMapper.selectList(classWrapper);
                for (com.autograding.entity.Class cls : classes) {
                    LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
                    csWrapper.eq(ClassStudent::getClassId, cls.getId());
                    classStudentMapper.delete(csWrapper);
                    LambdaUpdateWrapper<com.autograding.entity.Class> clsUpdate = new LambdaUpdateWrapper<com.autograding.entity.Class>()
                            .eq(com.autograding.entity.Class::getId, cls.getId())
                            .set(com.autograding.entity.Class::getDeleted, 1)
                            .set(com.autograding.entity.Class::getUpdatedAt, LocalDateTime.now());
                    classMapper.update(null, clsUpdate);
                }
                LambdaUpdateWrapper<Course> courseUpdate = new LambdaUpdateWrapper<Course>()
                        .eq(Course::getId, course.getId())
                        .set(Course::getDeleted, 1)
                        .set(Course::getUpdatedAt, LocalDateTime.now());
                courseMapper.update(null, courseUpdate);
            }
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getDeleted, 1)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "DELETE_USER", "USER", userId,
                "管理员删除用户: " + user.getUsername(), null);
    }

    public void resetPassword(Long userId, String newPassword) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "RESET_PASSWORD", "USER", userId,
                "管理员重置用户密码", null);
    }

    public void disableUser(Long userId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, 0)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "DISABLE_USER", "USER", userId,
                "管理员禁用用户", null);
    }

    public void enableUser(Long userId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, 1)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "ENABLE_USER", "USER", userId,
                "管理员启用用户", null);
    }
}
