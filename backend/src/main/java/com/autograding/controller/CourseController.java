package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.dto.course.CourseCreateRequest;
import com.autograding.dto.course.CourseResponse;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.CourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final UserMapper userMapper;

    public CourseController(CourseService courseService, UserMapper userMapper) {
        this.courseService = courseService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public Result<CourseResponse> createCourse(@RequestBody CourseCreateRequest request) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(courseService.createCourse(request, teacherId));
    }

    @GetMapping
    public Result<List<CourseResponse>> getAllCourses() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user.getRole() == User.Role.TEACHER) {
            return Result.success(courseService.getCoursesByTeacher(userId));
        }
        return Result.success(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public Result<CourseResponse> getCourseById(@PathVariable Long id) {
        return Result.success(courseService.getCourseById(id));
    }

    @GetMapping("/teacher")
    public Result<List<CourseResponse>> getMyCourses() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(courseService.getCoursesByTeacher(teacherId));
    }

    @PutMapping("/{id}")
    public Result<CourseResponse> updateCourse(@PathVariable Long id, @RequestBody CourseCreateRequest request) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(courseService.updateCourse(id, request, teacherId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        courseService.deleteCourse(id, teacherId);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> adminDeleteCourse(@PathVariable Long id) {
        courseService.adminDeleteCourse(id);
        return Result.success(null);
    }

}
