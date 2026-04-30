package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.dto.course.CourseCreateRequest;
import com.autograding.dto.course.CourseResponse;
import com.autograding.entity.Course;
import com.autograding.entity.User;
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

    public CourseService(CourseMapper courseMapper, UserMapper userMapper, OperationLogService operationLogService) {
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
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

        course.setTeacher(teacher);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "CREATE_COURSE", "COURSE", course.getId(),
                "创建课程: " + course.getName(), null);
        return CourseResponse.fromEntity(course);
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

    private CourseResponse enrichAndConvert(Course course) {
        if (course.getTeacher() == null && course.getTeacherId() != null) {
            User teacher = userMapper.selectById(course.getTeacherId());
            course.setTeacher(teacher);
        }
        return CourseResponse.fromEntity(course);
    }
}
