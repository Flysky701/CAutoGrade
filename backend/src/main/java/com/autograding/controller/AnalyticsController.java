package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/class/{classId}")
    public Result<Map<String, Object>> getClassAnalytics(@PathVariable Long classId) {
        return Result.success(analyticsService.getClassAnalytics(classId));
    }

    @GetMapping("/student/{studentId}")
    public Result<Map<String, Object>> getStudentAnalytics(@PathVariable Long studentId) {
        return Result.success(analyticsService.getStudentAnalytics(studentId));
    }

    @GetMapping("/assignment/{assignmentId}")
    public Result<Map<String, Object>> getAssignmentAnalytics(@PathVariable Long assignmentId) {
        return Result.success(analyticsService.getAssignmentAnalytics(assignmentId));
    }

    @GetMapping("/problem/{problemId}")
    public Result<Map<String, Object>> getProblemAnalytics(@PathVariable Long problemId) {
        return Result.success(analyticsService.getProblemAnalytics(problemId));
    }
}
