package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
               .eq(User::getDeleted, 0);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    public List<User> getAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
               .orderByAsc(User::getRole)
               .orderByDesc(User::getCreatedAt);
        return userMapper.selectList(wrapper);
    }

    public List<User> getUsersByRole(User.Role role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, role)
               .eq(User::getDeleted, 0)
               .orderByDesc(User::getCreatedAt);
        return userMapper.selectList(wrapper);
    }

    public User updateProfile(Long userId, String nickname, String avatar, String code) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(nickname != null, User::getNickname, nickname)
                .set(avatar != null, User::getAvatar, avatar)
                .set(code != null, User::getCode, code)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
        return getUserById(userId);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    public void resetPassword(Long userId, String newPassword) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPasswordHash, passwordEncoder.encode(newPassword))
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    public void disableUser(Long userId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, 0)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    public void enableUser(Long userId) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, 1)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, wrapper);
    }
}
