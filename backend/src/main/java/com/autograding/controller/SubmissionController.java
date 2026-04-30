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
    public Result<List<java.util.Map<String, Object>>> getMySubmissions() {
        Long studentId = SecurityUtils.getCurrentUserId();
        List<Submission> submissions = submissionService.getSubmissionsByStudent(studentId);
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (Submission s : submissions) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", s.getId());
            map.put("assignmentId", s.getAssignmentId());
            map.put("problemId", s.getProblemId());
            map.put("codeContent", s.getCodeContent());
            map.put("language", s.getLanguage());
            map.put("submitCount", s.getSubmitCount());
            map.put("isLate", s.getIsLate());
            map.put("submittedAt", s.getSubmittedAt());
            GradingResult gr = submissionService.getGradingResultBySubmission(s.getId());
            if (gr != null) {
                map.put("gradingStatus", gr.getGradingStatus());
                map.put("totalScore", gr.getTotalScore());
                map.put("humanAdjustedScore", gr.getHumanAdjustedScore());
            }
            result.add(map);
        }
        return Result.success(result);
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
