package com.autograding.service;

import com.autograding.common.BusinessException;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User student;
    private User teacher;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setUsername("student1");
        student.setNickname("小明");
        student.setPasswordHash("$2a$10$oldhash");
        student.setRole(User.Role.STUDENT);
        student.setStatus(1);
        student.setDeleted(0);

        teacher = new User();
        teacher.setId(2L);
        teacher.setUsername("teacher1");
        teacher.setNickname("张老师");
        teacher.setRole(User.Role.TEACHER);
        teacher.setStatus(1);
        teacher.setDeleted(0);
    }

    @Test
    void getUserById_shouldSucceed() {
        when(userMapper.selectById(1L)).thenReturn(student);

        User result = userService.getUserById(1L);

        assertEquals("student1", result.getUsername());
        assertEquals("小明", result.getNickname());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> userService.getUserById(99L));
    }

    @Test
    void getUserById_shouldThrowWhenDeleted() {
        student.setDeleted(1);
        when(userMapper.selectById(1L)).thenReturn(student);

        assertThrows(BusinessException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void getUserByUsername_shouldSucceed() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(student);

        User result = userService.getUserByUsername("student1");

        assertEquals("student1", result.getUsername());
    }

    @Test
    void getUserByUsername_shouldThrowWhenNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> userService.getUserByUsername("nobody"));
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(student, teacher));

        List<User> results = userService.getAllUsers();

        assertEquals(2, results.size());
    }

    @Test
    void getUsersByRole_shouldReturnFilteredList() {
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(student));

        List<User> results = userService.getUsersByRole(User.Role.STUDENT);

        assertEquals(1, results.size());
        assertEquals(User.Role.STUDENT, results.get(0).getRole());
    }

    @Test
    void changePassword_shouldThrowWhenWrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(student);
        when(passwordEncoder.matches("wrongPass", student.getPasswordHash())).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, "wrongPass", "newPass"));
    }
}
