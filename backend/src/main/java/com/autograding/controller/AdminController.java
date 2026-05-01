package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.OperationLog;
import com.autograding.entity.User;
import com.autograding.security.SecurityUtils;
import com.autograding.service.OperationLogService;
import com.autograding.service.SystemConfigService;
import com.autograding.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OperationLogService operationLogService;
    private final UserService userService;
    private final SystemConfigService systemConfigService;

    public AdminController(OperationLogService operationLogService, UserService userService,
                           SystemConfigService systemConfigService) {
        this.operationLogService = operationLogService;
        this.userService = userService;
        this.systemConfigService = systemConfigService;
    }

    // ==================== 系统配置 ====================

    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.success(systemConfigService.getAllConfig());
    }

    @PutMapping("/config/llm")
    public Result<Map<String, Object>> updateLlmConfig(@RequestBody Map<String, Object> config) {
        systemConfigService.updateConfig("llm", config);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "UPDATE_CONFIG", "SYSTEM", null,
                "更新LLM配置", null);
        return Result.success(systemConfigService.getAllConfig());
    }

    @PutMapping("/config/sandbox")
    public Result<Map<String, Object>> updateSandboxConfig(@RequestBody Map<String, Object> config) {
        systemConfigService.updateConfig("sandbox", config);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "UPDATE_CONFIG", "SYSTEM", null,
                "更新沙箱配置", null);
        return Result.success(systemConfigService.getAllConfig());
    }

    @PutMapping("/config/scoring")
    public Result<Map<String, Object>> updateScoringConfig(@RequestBody Map<String, Object> config) {
        systemConfigService.updateConfig("scoring", config);
        operationLogService.logOperation(SecurityUtils.getCurrentUserId(), "UPDATE_CONFIG", "SYSTEM", null,
                "更新评分配置", null);
        return Result.success(systemConfigService.getAllConfig());
    }

    // ==================== 操作日志 ====================

    @GetMapping("/logs")
    public Result<List<OperationLog>> getRecentLogs(@RequestParam(defaultValue = "100") int limit) {
        return Result.success(operationLogService.getRecentLogs(limit));
    }

    @GetMapping("/logs/user/{userId}")
    public Result<List<OperationLog>> getLogsByUser(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "50") int limit) {
        return Result.success(operationLogService.getLogsByUser(userId, limit));
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            return Result.success(userService.getUsersByRole(User.Role.valueOf(role.toUpperCase())));
        }
        return Result.success(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(userService.createUserByAdmin(user));
    }

    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return Result.success(userService.updateUserByAdmin(id, user));
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }
}
