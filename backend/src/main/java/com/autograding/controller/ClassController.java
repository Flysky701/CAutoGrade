package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.dto.course.ClassCreateRequest;
import com.autograding.dto.course.ClassResponse;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.ClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;
    private final UserMapper userMapper;

    public ClassController(ClassService classService, UserMapper userMapper) {
        this.classService = classService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public Result<ClassResponse> createClass(@RequestBody ClassCreateRequest request) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(classService.createClass(request, teacherId));
    }

    @GetMapping("/course/{courseId}")
    public Result<List<ClassResponse>> getClassesByCourse(@PathVariable Long courseId) {
        return Result.success(classService.getClassesByCourse(courseId));
    }

    @GetMapping("/teacher")
    public Result<List<ClassResponse>> getMyClasses() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(classService.getClassesByTeacher(teacherId));
    }

    @GetMapping("/{id}")
    public Result<ClassResponse> getClassById(@PathVariable Long id) {
        return Result.success(classService.getClassById(id));
    }

    @PostMapping("/join")
    public Result<ClassResponse> joinClass(@RequestParam String inviteCode) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(classService.joinClassByCode(inviteCode, studentId));
    }

    @GetMapping("/student")
    public Result<List<ClassResponse>> getMyClassesAsStudent() {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(classService.getClassesByStudent(studentId));
    }

    @PostMapping("/{classId}/students/{studentId}")
    public Result<Void> addStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        classService.addStudentToClass(classId, studentId);
        return Result.success(null);
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        classService.removeStudentFromClass(classId, studentId);
        return Result.success(null);
    }

    @GetMapping("/{classId}/students")
    public Result<List<User>> getClassStudents(@PathVariable Long classId) {
        return Result.success(classService.getClassStudents(classId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        classService.deleteClass(id, teacherId);
        return Result.success(null);
    }

}
