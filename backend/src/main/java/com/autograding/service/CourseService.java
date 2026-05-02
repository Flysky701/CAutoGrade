package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.dto.course.CourseCreateRequest;
import com.autograding.dto.course.CourseResponse;
import com.autograding.entity.Class;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;

    public CourseService(CourseMapper courseMapper, UserMapper userMapper, OperationLogService operationLogService,
                         ClassMapper classMapper, ClassStudentMapper classStudentMapper) {
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.classMapper = classMapper;
        this.classStudentMapper = classStudentMapper;
    }

    public CourseResponse createCourse(CourseCreateRequest request, Long teacherId) {
        User teacher = userMapper.selectById(teacherId);
        if (teacher == null || teacher.getRole() != User.Role.TEACHER) {
            throw new BusinessException("无效的教师ID");
        }

        Course course = new Course();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setTeacherId(teacherId);
        course.setSemester(request.getSemester());
        course.setCoverUrl(request.getCoverUrl());
        course.setInviteCode(generateInviteCode());
        course.setStatus(Course.Status.ACTIVE);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.insert(course);

        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "CREATE_COURSE", "COURSE", course.getId(),
                "创建课程: " + course.getName(), null);
        return CourseResponse.fromEntity(course, teacher);
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public List<CourseResponse> getCoursesByTeacher(Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getTeacherId, teacherId)
               .eq(Course::getDeleted, 0)
               .orderByDesc(Course::getCreatedAt);
        return courseMapper.selectList(wrapper).stream()
                .map(this::enrichAndConvert)
                .collect(Collectors.toList());
    }

    public List<CourseResponse> getAllCourses() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getStatus, Course.Status.ACTIVE)
               .eq(Course::getDeleted, 0)
               .orderByDesc(Course::getCreatedAt);
        return courseMapper.selectList(wrapper).stream()
                .map(this::enrichAndConvert)
                .collect(Collectors.toList());
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }
        return enrichAndConvert(course);
    }

    public CourseResponse updateCourse(Long id, CourseCreateRequest request, Long teacherId) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限修改此课程");
        }

        LambdaUpdateWrapper<Course> wrapper = new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .set(request.getName() != null, Course::getName, request.getName())
                .set(request.getDescription() != null, Course::getDescription, request.getDescription())
                .set(request.getSemester() != null, Course::getSemester, request.getSemester())
                .set(request.getCoverUrl() != null, Course::getCoverUrl, request.getCoverUrl())
                .set(Course::getUpdatedAt, LocalDateTime.now());
        courseMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "UPDATE_COURSE", "COURSE", id,
                "更新课程", null);
        return getCourseById(id);
    }

    public void deleteCourse(Long id, Long teacherId) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限删除此课程");
        }

        LambdaUpdateWrapper<Course> wrapper = new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .set(Course::getDeleted, 1)
                .set(Course::getUpdatedAt, LocalDateTime.now());
        courseMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "DELETE_COURSE", "COURSE", id,
                "删除课程", null);
    }

    public void adminDeleteCourse(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted() == 1) {
            throw new BusinessException("课程不存在");
        }

        LambdaQueryWrapper<Class> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(Class::getCourseId, id)
                    .eq(Class::getDeleted, 0);
        List<Class> classes = classMapper.selectList(classWrapper);
        for (Class cls : classes) {
            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getClassId, cls.getId());
            classStudentMapper.delete(csWrapper);

            LambdaUpdateWrapper<Class> clsUpdate = new LambdaUpdateWrapper<Class>()
                    .eq(Class::getId, cls.getId())
                    .set(Class::getDeleted, 1)
                    .set(Class::getUpdatedAt, LocalDateTime.now());
            classMapper.update(null, clsUpdate);
        }

        LambdaUpdateWrapper<Course> wrapper = new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, id)
                .set(Course::getDeleted, 1)
                .set(Course::getUpdatedAt, LocalDateTime.now());
        courseMapper.update(null, wrapper);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "ADMIN_DELETE_COURSE", "COURSE", id,
                "管理员删除课程（含关联班级）", null);
    }

    private CourseResponse enrichAndConvert(Course course) {
        User teacher = null;
        if (course.getTeacherId() != null) {
            teacher = userMapper.selectById(course.getTeacherId());
        }
        return CourseResponse.fromEntity(course, teacher);
    }
}
