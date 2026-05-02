package com.autograding.service;

import com.autograding.entity.*;
import com.autograding.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private GradingResultMapper gradingResultMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private ClassStudentMapper classStudentMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    private ClassStudent classStudent;
    private Submission submission;
    private GradingResult gradingResult;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        classStudent = new ClassStudent();
        classStudent.setId(1L);
        classStudent.setClassId(10L);
        classStudent.setStudentId(100L);

        submission = new Submission();
        submission.setId(1L);
        submission.setStudentId(100L);
        submission.setAssignmentId(5L);
        submission.setProblemId(20L);
        submission.setDeleted(0);

        gradingResult = new GradingResult();
        gradingResult.setId(1L);
        gradingResult.setSubmissionId(1L);
        gradingResult.setTotalScore(new BigDecimal("85.00"));
        gradingResult.setGradingStatus(GradingResult.GradingStatus.DONE);

        assignment = new Assignment();
        assignment.setId(5L);
        assignment.setTitle("测试作业");
        assignment.setMaxScore(100);
    }

    @Test
    void getClassAnalytics_shouldReturnZeroWhenNoStudents() {
        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = analyticsService.getClassAnalytics(10L);

        assertEquals(0, result.get("totalStudents"));
        assertEquals(0.0, result.get("submissionRate"));
        assertEquals(0.0, result.get("averageScore"));
    }

    @Test
    void getClassAnalytics_shouldReturnStats() {
        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(classStudent));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any()))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getClassAnalytics(10L);

        assertEquals(1, result.get("totalStudents"));
        assertTrue((Double) result.get("submissionRate") >= 0);
        assertTrue((Double) result.get("averageScore") >= 0);
    }

    @Test
    void getStudentAnalytics_shouldReturnStats() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getStudentAnalytics(100L);

        assertEquals(1, result.get("totalSubmissions"));
        assertEquals(85.0, (Double) result.get("averageScore"), 0.01);
        assertEquals(new BigDecimal("85.00"), result.get("highestScore"));
    }

    @Test
    void getStudentAnalytics_shouldReturnZeroWhenNoSubmissions() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = analyticsService.getStudentAnalytics(100L);

        assertEquals(0, result.get("totalSubmissions"));
        assertEquals(0.0, (Double) result.get("averageScore"), 0.01);
    }

    @Test
    void getAssignmentAnalytics_shouldReturnEmptyWhenNotFound() {
        when(assignmentMapper.selectById(99L)).thenReturn(null);

        Map<String, Object> result = analyticsService.getAssignmentAnalytics(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAssignmentAnalytics_shouldReturnStats() {
        when(assignmentMapper.selectById(5L)).thenReturn(assignment);
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getAssignmentAnalytics(5L);

        assertEquals(1, result.get("totalSubmissions"));
        assertEquals(100, result.get("maxScore"));
        assertNotNull(result.get("averageScore"));
        assertNotNull(result.get("highestScore"));
        assertNotNull(result.get("lowestScore"));
    }

    @Test
    void getProblemAnalytics_shouldReturnStats() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getProblemAnalytics(20L);

        assertEquals(1, result.get("totalSubmissions"));
        assertTrue((Integer) result.get("passedCount") >= 0);
        assertTrue((Double) result.get("passRate") >= 0);
    }

    @Test
    void getProblemAnalytics_shouldReturnZeroWhenNoSubmissions() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = analyticsService.getProblemAnalytics(20L);

        assertEquals(0, result.get("totalSubmissions"));
        assertEquals(0, result.get("passedCount"));
        assertEquals(0.0, (Double) result.get("passRate"), 0.01);
    }

    @Test
    void getEffectiveScore_prefersHumanAdjusted() {
        gradingResult.setTotalScore(new BigDecimal("85.00"));
        gradingResult.setHumanAdjustedScore(new BigDecimal("90.00"));

        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(classStudent));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any()))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getClassAnalytics(10L);

        assertEquals(90.0, (Double) result.get("averageScore"), 0.01);
    }

    @Test
    void getEffectiveScore_fallsBackToTotalScore() {
        gradingResult.setTotalScore(new BigDecimal("85.00"));
        gradingResult.setHumanAdjustedScore(null);

        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(classStudent));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any()))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getClassAnalytics(10L);

        assertEquals(85.0, (Double) result.get("averageScore"), 0.01);
    }

    @Test
    void getEffectiveScore_bothNull_returnsZeroAverage() {
        gradingResult.setTotalScore(null);
        gradingResult.setHumanAdjustedScore(null);

        when(classStudentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(classStudent));
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any()))
                .thenReturn(List.of(gradingResult));

        Map<String, Object> result = analyticsService.getClassAnalytics(10L);

        assertEquals(0.0, (Double) result.get("averageScore"), 0.01);
    }
}
