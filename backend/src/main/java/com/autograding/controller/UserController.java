package com.autograding.controller;

import com.autograding.common.Result;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.SecurityUtils;
import com.autograding.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @GetMapping("/profile")
    public Result<User> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userService.getUserById(userId));
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody ProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userService.updateProfile(userId, request.getNickname(), request.getAvatar(), request.getCode()));
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success(null);
    }

    @GetMapping
    public Result<List<User>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    public Result<List<User>> getUsersByRole(@PathVariable String role) {
        return Result.success(userService.getUsersByRole(User.Role.valueOf(role.toUpperCase())));
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success(null);
    }


    public static class ProfileRequest {
        private String nickname;
        private String avatar;
        private String code;

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
