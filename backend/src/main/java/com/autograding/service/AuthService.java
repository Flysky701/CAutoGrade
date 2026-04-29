package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.dto.auth.AuthResponse;
import com.autograding.dto.auth.LoginRequest;
import com.autograding.dto.auth.RegisterRequest;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0)
                .last("limit 1"));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setCode(request.getCode());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(parseRole(request.getRole()));
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtTokenProvider.generateToken(userDetails);
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getCode());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BusinessException("用户名或密码错误");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getDeleted, 0)
                .last("limit 1"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtTokenProvider.generateToken(userDetails);
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getCode());
    }

    private User.Role parseRole(String role) {
        try {
            return User.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("角色非法，仅支持 STUDENT/TEACHER/ADMIN");
        }
    }
}
