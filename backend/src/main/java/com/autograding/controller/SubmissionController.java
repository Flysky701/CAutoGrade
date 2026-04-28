package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.security.SecurityUtils;
import com.autograding.service.SubmissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public Result<Submission> submitCode(@RequestBody SubmitCodeRequest request) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(submissionService.submitCode(
                request.getAssignmentId(),
                request.getProblemId(),
                studentId,
                request.getCode()
        ));
    }

    @GetMapping("/{id}")
    public Result<Submission> getSubmissionById(@PathVariable Long id) {
        return Result.success(submissionService.getSubmissionById(id));
    }

    @GetMapping("/{id}/grading")
    public Result<GradingResult> getGradingResult(@PathVariable Long id) {
        return Result.success(submissionService.getGradingResultBySubmission(id));
    }

    @GetMapping("/student")
    public Result<List<Submission>> getMySubmissions() {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(submissionService.getSubmissionsByStudent(studentId));
    }

    @GetMapping("/assignment/{assignmentId}")
    public Result<List<Submission>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        return Result.success(submissionService.getSubmissionsByAssignment(assignmentId));
    }

    @PutMapping("/grading/{id}/review")
    public Result<GradingResult> reviewGrading(@PathVariable Long id,
                                               @RequestParam BigDecimal adjustedScore,
                                               @RequestParam(required = false) String feedback) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(submissionService.reviewGrading(id, teacherId, adjustedScore, feedback));
    }

    public static class SubmitCodeRequest {
        private Long assignmentId;
        private Long problemId;
        private String code;

        public Long getAssignmentId() { return assignmentId; }
        public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
