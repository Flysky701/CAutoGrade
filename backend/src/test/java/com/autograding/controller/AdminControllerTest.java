package com.autograding.controller;

import com.autograding.entity.OperationLog;
import com.autograding.entity.User;
import com.autograding.service.OperationLogService;
import com.autograding.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OperationLogService operationLogService;
    @MockBean private UserService userService;
    @MockBean private com.autograding.security.JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsService userDetailsService;

    private User student;
    private OperationLog log;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(5L);
        student.setUsername("student1");
        student.setNickname("小明");
        student.setRole(User.Role.STUDENT);
        student.setStatus(1);

        log = new OperationLog();
        log.setId(1L);
        log.setUserId(10L);
        log.setAction("CREATE");
        log.setTargetType("COURSE");
        log.setTargetId(5L);
        log.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getRecentLogs_shouldReturnList() throws Exception {
        when(operationLogService.getRecentLogs(100)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].action").value("CREATE"));
    }

    @Test
    void getLogsByUser_shouldReturnList() throws Exception {
        when(operationLogService.getLogsByUser(10L, 50)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/logs/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].userId").value(10));
    }

    @Test
    void getAllUsers_shouldReturnList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].username").value("student1"));
    }

    @Test
    void getUsersByRole_shouldReturnFiltered() throws Exception {
        when(userService.getUsersByRole(User.Role.STUDENT)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/admin/users").param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].role").value("STUDENT"));
    }

    @Test
    void createUser_shouldSucceed() throws Exception {
        when(userService.createUserByAdmin(any(User.class))).thenReturn(student);

        String body = objectMapper.writeValueAsString(student);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("student1"));
    }

    @Test
    void updateUser_shouldSucceed() throws Exception {
        when(userService.updateUserByAdmin(eq(5L), any(User.class))).thenReturn(student);

        String body = objectMapper.writeValueAsString(student);

        mockMvc.perform(put("/api/admin/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteUser_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/admin/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
