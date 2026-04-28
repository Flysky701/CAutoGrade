package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.Assignment;
import com.autograding.entity.AssignmentProblem;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.AssignmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final UserMapper userMapper;

    public AssignmentController(AssignmentService assignmentService, UserMapper userMapper) {
        this.assignmentService = assignmentService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public Result<Assignment> createAssignment(@RequestBody AssignmentRequest request) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setCourseId(request.getCourseId());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setType(request.getType());
        assignment.setLatePenaltyScore(request.getLatePenaltyScore());
        return Result.success(assignmentService.createAssignment(assignment, teacherId, request.getProblemIds()));
    }

    @GetMapping("/course/{courseId}")
    public Result<List<Assignment>> getAssignmentsByCourse(@PathVariable Long courseId) {
        return Result.success(assignmentService.getAssignmentsByCourse(courseId));
    }

    @GetMapping("/teacher")
    public Result<List<Assignment>> getMyAssignments() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(assignmentService.getAssignmentsByTeacher(teacherId));
    }

    @GetMapping("/{id}")
    public Result<Assignment> getAssignmentById(@PathVariable Long id) {
        return Result.success(assignmentService.getAssignmentById(id));
    }

    @GetMapping("/{id}/problems")
    public Result<List<AssignmentProblem>> getAssignmentProblems(@PathVariable Long id) {
        return Result.success(assignmentService.getAssignmentProblems(id));
    }

    @PutMapping("/{id}")
    public Result<Assignment> updateAssignment(@PathVariable Long id, @RequestBody AssignmentRequest request) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setStartTime(request.getStartTime());
        assignment.setEndTime(request.getEndTime());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setType(request.getType());
        assignment.setLatePenaltyScore(request.getLatePenaltyScore());
        return Result.success(assignmentService.updateAssignment(id, assignment, teacherId, request.getProblemIds()));
    }

    @PostMapping("/{id}/publish")
    public Result<Assignment> publishAssignment(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(assignmentService.publishAssignment(id, teacherId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAssignment(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        assignmentService.deleteAssignment(id, teacherId);
        return Result.success(null);
    }


    public static class AssignmentRequest {
        private String title;
        private String description;
        private Long courseId;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private Integer maxScore;
        private Assignment.Type type;
        private Integer latePenaltyScore;
        private List<Long> problemIds;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }
        public java.time.LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(java.time.LocalDateTime startTime) { this.startTime = startTime; }
        public java.time.LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(java.time.LocalDateTime endTime) { this.endTime = endTime; }
        public Integer getMaxScore() { return maxScore; }
        public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }
        public Assignment.Type getType() { return type; }
        public void setType(Assignment.Type type) { this.type = type; }
        public Integer getLatePenaltyScore() { return latePenaltyScore; }
        public void setLatePenaltyScore(Integer latePenaltyScore) { this.latePenaltyScore = latePenaltyScore; }
        public List<Long> getProblemIds() { return problemIds; }
        public void setProblemIds(List<Long> problemIds) { this.problemIds = problemIds; }
    }
}
