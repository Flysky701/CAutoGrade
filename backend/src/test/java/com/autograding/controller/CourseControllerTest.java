package com.autograding.controller;

import com.autograding.common.BusinessException;
import com.autograding.dto.course.CourseCreateRequest;
import com.autograding.dto.course.CourseResponse;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.service.CourseService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CourseController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CourseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CourseService courseService;
    @MockBean private UserMapper userMapper;
    @MockBean private com.autograding.security.JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsService userDetailsService;

    private User teacher;

    private CourseResponse buildResponse(Long id, String name) {
        CourseResponse r = new CourseResponse();
        r.setId(id);
        r.setName(name);
        r.setDescription("测试课程");
        r.setSemester("2026-SPRING");
        r.setTeacherName("张老师");
        r.setStatus(com.autograding.entity.Course.Status.ACTIVE);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher1");
        teacher.setRole(User.Role.TEACHER);
    }

    private void setAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
            new org.springframework.security.core.userdetails.User("teacher1", "", List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))),
            null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userMapper.selectById(1L)).thenReturn(teacher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCourse_shouldReturnCourse() throws Exception {
        setAuth();
        CourseResponse response = buildResponse(10L, "C语言程序设计");
        when(courseService.createCourse(any(CourseCreateRequest.class), eq(1L))).thenReturn(response);

        CourseCreateRequest request = new CourseCreateRequest();
        request.setName("C语言程序设计");
        request.setSemester("2026-SPRING");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("C语言程序设计"));
    }

    @Test
    void getAllCourses_shouldReturnTeacherCourses() throws Exception {
        setAuth();
        when(courseService.getCoursesByTeacher(eq(1L)))
                .thenReturn(List.of(buildResponse(10L, "C语言")));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("C语言"));
    }

    @Test
    void getCourseById_shouldReturnCourse() throws Exception {
        when(courseService.getCourseById(10L)).thenReturn(buildResponse(10L, "C语言程序设计"));

        mockMvc.perform(get("/api/courses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("C语言程序设计"));
    }

    @Test
    void getCourseById_shouldHandleNotFound() throws Exception {
        when(courseService.getCourseById(99L))
                .thenThrow(new BusinessException("课程不存在"));

        mockMvc.perform(get("/api/courses/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void deleteCourse_shouldSucceed() throws Exception {
        setAuth();
        mockMvc.perform(delete("/api/courses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
