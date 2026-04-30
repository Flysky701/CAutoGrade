package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.OperationLog;
import com.autograding.entity.User;
import com.autograding.service.OperationLogService;
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

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OperationLogService operationLogService;
    private final UserService userService;

    public AdminController(OperationLogService operationLogService, UserService userService) {
        this.operationLogService = operationLogService;
        this.userService = userService;
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

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<List<User>> getAllUsers(@RequestParam(required = false) String role) {
        if (role != null && !role.isBlank()) {
            return Result.success(userService.getUsersByRole(User.Role.valueOf(role.toUpperCase())));
        }
        return Result.success(userService.getAllUsers());
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
