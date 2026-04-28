package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.Problem;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.ProblemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final UserMapper userMapper;

    public ProblemController(ProblemService problemService, UserMapper userMapper) {
        this.problemService = problemService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public Result<Problem> createProblem(@RequestBody Problem problem) {
        Long creatorId = SecurityUtils.getCurrentUserId();
        return Result.success(problemService.createProblem(problem, creatorId));
    }

    @GetMapping
    public Result<List<Problem>> getMyProblems() {
        Long creatorId = SecurityUtils.getCurrentUserId();
        return Result.success(problemService.getProblemsByCreator(creatorId));
    }

    @GetMapping("/public")
    public Result<List<Problem>> getPublicProblems() {
        return Result.success(problemService.getPublicProblems());
    }

    @GetMapping("/{id}")
    public Result<Problem> getProblemById(@PathVariable Long id) {
        return Result.success(problemService.getProblemById(id));
    }

    @PutMapping("/{id}")
    public Result<Problem> updateProblem(@PathVariable Long id, @RequestBody Problem request) {
        Long creatorId = SecurityUtils.getCurrentUserId();
        return Result.success(problemService.updateProblem(id, request, creatorId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProblem(@PathVariable Long id) {
        Long creatorId = SecurityUtils.getCurrentUserId();
        problemService.deleteProblem(id, creatorId);
        return Result.success(null);
    }

}
