package com.autograding.controller;

import com.autograding.common.BusinessException;
import com.autograding.dto.course.ClassCreateRequest;
import com.autograding.dto.course.ClassResponse;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.service.ClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ClassController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = com.autograding.config.SecurityConfig.class))
class ClassControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ClassService classService;
    @MockBean private UserMapper userMapper;
    @MockBean private com.autograding.security.JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsService userDetailsService;

    private ClassResponse classResponse;
    private User mockUser;
    private AutoCloseable mockStaticCloseable;

    @BeforeEach
    void setUp() {
        classResponse = new ClassResponse();
        classResponse.setId(100L);
        classResponse.setName("一班");
        classResponse.setCourseId(10L);
        classResponse.setInviteCode("ABC12345");
        classResponse.setStudentCount(5);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("teacher1");
        mockUser.setRole(User.Role.TEACHER);
    }

    private void setAuth() {
        mockStaticCloseable = mockStatic(com.autograding.security.SecurityUtils.class);
        when(com.autograding.security.SecurityUtils.getCurrentUserId()).thenReturn(1L);
        when(com.autograding.security.SecurityUtils.requireCurrentUserId()).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (mockStaticCloseable != null) {
            try { mockStaticCloseable.close(); } catch (Exception e) { /* ignore */ }
            mockStaticCloseable = null;
        }
    }

    @Test
    void createClass_shouldReturnClass() throws Exception {
        setAuth();
        when(classService.createClass(any(ClassCreateRequest.class), eq(1L)))
                .thenReturn(classResponse);

        ClassCreateRequest request = new ClassCreateRequest();
        request.setName("一班");
        request.setCourseId(10L);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("一班"));
    }

    @Test
    void getClassesByCourse_shouldReturnList() throws Exception {
        when(classService.getClassesByCourse(10L)).thenReturn(List.of(classResponse));

        mockMvc.perform(get("/api/classes/course/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("一班"));
    }

    @Test
    void getClassById_shouldReturnClass() throws Exception {
        when(classService.getClassById(100L)).thenReturn(classResponse);

        mockMvc.perform(get("/api/classes/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.inviteCode").value("ABC12345"));
    }

    @Test
    void joinClass_shouldSucceed() throws Exception {
        setAuth();
        when(classService.joinClassByCode(eq("ABC12345"), eq(1L)))
                .thenReturn(classResponse);

        mockMvc.perform(post("/api/classes/join")
                        .param("inviteCode", "ABC12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("一班"));
    }

    @Test
    void joinClass_shouldHandleInvalidCode() throws Exception {
        setAuth();
        when(classService.joinClassByCode(eq("INVALID"), eq(1L)))
                .thenThrow(new BusinessException("邀请码无效"));

        mockMvc.perform(post("/api/classes/join")
                        .param("inviteCode", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addStudent_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/classes/100/students/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void removeStudent_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/classes/100/students/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getClassStudents_shouldReturnList() throws Exception {
        User student = new User();
        student.setId(5L);
        student.setUsername("student1");
        student.setNickname("小明");
        student.setRole(User.Role.STUDENT);

        when(classService.getClassStudents(100L)).thenReturn(List.of(student));

        mockMvc.perform(get("/api/classes/100/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].nickname").value("小明"));
    }

    @Test
    void deleteClass_shouldSucceed() throws Exception {
        setAuth();
        mockMvc.perform(delete("/api/classes/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
