package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.OperationLog;
import com.autograding.service.OperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OperationLogService operationLogService;

    public AdminController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/logs")
    public Result<List<OperationLog>> getRecentLogs(@RequestParam(defaultValue = "100") int limit) {
        return Result.success(operationLogService.getRecentLogs(limit));
    }

    @GetMapping("/logs/user/{userId}")
    public Result<List<OperationLog>> getLogsByUser(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "50") int limit) {
        return Result.success(operationLogService.getLogsByUser(userId, limit));
    }
}
