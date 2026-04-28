package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.TestCase;
import com.autograding.service.TestCaseService;
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
@RequestMapping("/api/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping
    public Result<TestCase> createTestCase(@RequestBody TestCase testCase) {
        return Result.success(testCaseService.createTestCase(testCase));
    }

    @GetMapping("/problem/{problemId}")
    public Result<List<TestCase>> getTestCasesByProblem(@PathVariable Long problemId) {
        return Result.success(testCaseService.getTestCasesByProblem(problemId));
    }

    @GetMapping("/problem/{problemId}/visible")
    public Result<List<TestCase>> getVisibleTestCases(@PathVariable Long problemId) {
        return Result.success(testCaseService.getVisibleTestCases(problemId));
    }

    @GetMapping("/{id}")
    public Result<TestCase> getTestCaseById(@PathVariable Long id) {
        return Result.success(testCaseService.getTestCaseById(id));
    }

    @PutMapping("/{id}")
    public Result<TestCase> updateTestCase(@PathVariable Long id, @RequestBody TestCase request) {
        return Result.success(testCaseService.updateTestCase(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return Result.success(null);
    }
}
