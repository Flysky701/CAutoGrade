package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.GradingResult;
import com.autograding.service.GradingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gradings")
public class GradingController {

    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @GetMapping("/pending")
    public Result<List<GradingResult>> getPendingGradings() {
        return Result.success(gradingService.getPendingGradings());
    }

    @GetMapping("/assignment/{assignmentId}")
    public Result<List<GradingResult>> getGradingsByAssignment(@PathVariable Long assignmentId) {
        return Result.success(gradingService.getGradingsByAssignment(assignmentId));
    }

    @GetMapping("/unreviewed")
    public Result<List<Map<String, Object>>> getUnreviewedGradings() {
        return Result.success(gradingService.getUnreviewedGradings());
    }

    @GetMapping("/submission/{submissionId}")
    public Result<GradingResult> getGradingResultBySubmissionId(@PathVariable Long submissionId) {
        return Result.success(gradingService.getGradingResultBySubmissionId(submissionId));
    }
}
