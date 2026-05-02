package com.autograding.controller;

import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.entity.User;
import com.autograding.mapper.UserMapper;
import com.autograding.service.SubmissionService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SubmissionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = com.autograding.config.SecurityConfig.class))
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private SubmissionService submissionService;
    @MockBean
    private com.autograding.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private UserMapper userMapper;

    private AutoCloseable mockStaticCloseable;

    private Submission submission;
    private GradingResult gradingResult;
    private User student;

    @BeforeEach
    void setUp() {
        submission = new Submission();
        submission.setId(1L);
        submission.setAssignmentId(5L);
        submission.setProblemId(20L);
        submission.setStudentId(100L);
        submission.setCodeContent("#include <stdio.h>\nint main() { return 0; }");
        submission.setLanguage("c");
        submission.setSubmitCount(1);
        submission.setIsLate(0);
        submission.setSubmittedAt(LocalDateTime.now());

        gradingResult = new GradingResult();
        gradingResult.setId(1L);
        gradingResult.setSubmissionId(1L);
        gradingResult.setTotalScore(new BigDecimal("85.00"));
        gradingResult.setGradingStatus(GradingResult.GradingStatus.DONE);

        student = new User();
        student.setId(100L);
        student.setUsername("student1");
        student.setRole(User.Role.STUDENT);
    }

    private void setStudentAuth() {
        mockStaticCloseable = mockStatic(com.autograding.security.SecurityUtils.class);
        when(com.autograding.security.SecurityUtils.getCurrentUserId()).thenReturn(100L);
        when(com.autograding.security.SecurityUtils.requireCurrentUserId()).thenReturn(100L);
    }

    private void setTeacherAuth() {
        mockStaticCloseable = mockStatic(com.autograding.security.SecurityUtils.class);
        when(com.autograding.security.SecurityUtils.getCurrentUserId()).thenReturn(2L);
        when(com.autograding.security.SecurityUtils.requireCurrentUserId()).thenReturn(2L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (mockStaticCloseable != null) {
            try {
                mockStaticCloseable.close();
            } catch (Exception e) {
                /* ignore */ }
            mockStaticCloseable = null;
        }
    }

    @Test
    void submitCode_shouldSucceed() throws Exception {
        setStudentAuth();
        when(submissionService.submitCode(eq(5L), eq(20L), eq(100L), anyString(), any()))
                .thenReturn(submission);

        String body = objectMapper.writeValueAsString(
                Map.of("assignmentId", 5, "problemId", 20, "code", "#include <stdio.h>\nint main() { return 0; }"));

        mockMvc.perform(post("/api/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assignmentId").value(5))
                .andExpect(jsonPath("$.data.problemId").value(20));
    }

    @Test
    void getSubmissionById_shouldReturnSubmission() throws Exception {
        when(submissionService.getSubmissionById(1L)).thenReturn(submission);

        mockMvc.perform(get("/api/submissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getGradingResult_shouldReturnResult() throws Exception {
        when(submissionService.getGradingResultBySubmission(1L)).thenReturn(gradingResult);

        mockMvc.perform(get("/api/submissions/1/grading"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalScore").value(85.00));
    }

    @Test
    void getMySubmissions_shouldReturnList() throws Exception {
        setStudentAuth();
        when(submissionService.getSubmissionsByStudent(eq(100L)))
                .thenReturn(List.of(submission));
        when(submissionService.getGradingResultBySubmission(anyLong()))
                .thenReturn(gradingResult);

        mockMvc.perform(get("/api/submissions/student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].gradingStatus").value("DONE"));
    }

    @Test
    void getSubmissionsByAssignment_shouldReturnList() throws Exception {
        when(submissionService.getSubmissionsByAssignment(5L)).thenReturn(List.of(submission));

        mockMvc.perform(get("/api/submissions/assignment/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].assignmentId").value(5));
    }

    @Test
    void reviewGrading_shouldSucceed() throws Exception {
        setTeacherAuth();
        GradingResult reviewed = new GradingResult();
        reviewed.setId(1L);
        reviewed.setTotalScore(new BigDecimal("85.00"));
        reviewed.setHumanAdjustedScore(new BigDecimal("90.00"));
        reviewed.setReviewedBy(2L);

        when(submissionService.reviewGrading(eq(1L), anyLong(), any(BigDecimal.class), anyString()))
                .thenReturn(reviewed);

        mockMvc.perform(put("/api/submissions/grading/1/review")
                .param("adjustedScore", "90.00")
                .param("feedback", "批阅合理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.humanAdjustedScore").value(90.00));
    }
}
