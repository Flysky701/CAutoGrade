package com.autograding.security;

import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("student1");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setRole(User.Role.STUDENT);
        user.setStatus(1);
        user.setDeleted(0);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("student1");

        assertEquals("student1", result.getUsername());
        assertEquals("$2a$10$hashedpassword", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
        assertTrue(result.isEnabled());
    }

    @Test
    void loadUserByUsername_shouldLoadByCodeWhenUsernameNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null, user);

        UserDetails result = userDetailsService.loadUserByUsername("S2026001");

        assertEquals("student1", result.getUsername());
        assertEquals("ROLE_STUDENT",
                result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nobody"));
    }

    @Test
    void loadUserByUsername_shouldMapTeacherRole() {
        user.setRole(User.Role.TEACHER);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("teacher1");

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER")));
    }

    @Test
    void loadUserByUsername_shouldMapAdminRole() {
        user.setRole(User.Role.ADMIN);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("admin");

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_shouldHandleDisabledUser() {
        user.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("disabled");

        assertFalse(result.isEnabled());
    }
}
