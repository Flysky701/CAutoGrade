package com.autograding.service;

import com.autograding.dto.auth.AuthResponse;
import com.autograding.dto.auth.LoginRequest;
import com.autograding.dto.auth.RegisterRequest;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setNickname("测试用户");
        registerRequest.setRole("STUDENT");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encoded")
                .roles("STUDENT")
                .build();
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);
        when(jwtTokenProvider.generateToken(mockUserDetails)).thenReturn("jwt.token.string");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt.token.string", response.getToken());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        User existing = new User();
        existing.setUsername("testuser");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        assertThrows(com.autograding.common.BusinessException.class,
                () -> authService.register(registerRequest));
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(User.Role.STUDENT);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);
        when(jwtTokenProvider.generateToken(mockUserDetails)).thenReturn("jwt.token.string");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("STUDENT", response.getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
