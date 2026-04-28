package com.autograding.security;

import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static UserMapper userMapper;

    public SecurityUtils(UserMapper userMapper) {
        SecurityUtils.userMapper = userMapper;
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }

    public static Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null) return null;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0)
                .last("limit 1"));
        return user != null ? user.getId() : null;
    }

    public static User getCurrentUser() {
        String username = getCurrentUsername();
        if (username == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0)
                .last("limit 1"));
    }
}
