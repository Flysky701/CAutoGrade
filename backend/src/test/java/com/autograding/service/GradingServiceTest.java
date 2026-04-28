package com.autograding.service;

import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.mapper.GradingResultMapper;
import com.autograding.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradingServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private GradingResultMapper gradingResultMapper;

    @InjectMocks
    private GradingService gradingService;

    private GradingResult pendingResult;
    private GradingResult doneResult;
    private Submission submission;

    @BeforeEach
    void setUp() {
        submission = new Submission();
        submission.setId(1L);
        submission.setAssignmentId(10L);

        pendingResult = new GradingResult();
        pendingResult.setId(1L);
        pendingResult.setSubmissionId(1L);
        pendingResult.setGradingStatus(GradingResult.GradingStatus.PENDING);

        doneResult = new GradingResult();
        doneResult.setId(2L);
        doneResult.setSubmissionId(2L);
        doneResult.setTotalScore(new BigDecimal("85.00"));
        doneResult.setGradingStatus(GradingResult.GradingStatus.DONE);
    }

    @Test
    void getPendingGradings_shouldReturnList() {
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(pendingResult));

        List<GradingResult> results = gradingService.getPendingGradings();

        assertEquals(1, results.size());
        assertEquals(GradingResult.GradingStatus.PENDING, results.get(0).getGradingStatus());
    }

    @Test
    void getPendingGradings_shouldReturnEmptyList() {
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<GradingResult> results = gradingService.getPendingGradings();

        assertTrue(results.isEmpty());
    }

    @Test
    void getGradingsByAssignment_shouldReturnResults() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(submission));
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(pendingResult));

        List<GradingResult> results = gradingService.getGradingsByAssignment(10L);

        assertEquals(1, results.size());
    }

    @Test
    void getGradingsByAssignment_shouldReturnEmptyWhenNoSubmissions() {
        when(submissionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<GradingResult> results = gradingService.getGradingsByAssignment(10L);

        assertTrue(results.isEmpty());
    }

    @Test
    void getUnreviewedGradings_shouldReturnList() {
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(doneResult));

        List<GradingResult> results = gradingService.getUnreviewedGradings();

        assertEquals(1, results.size());
        assertNull(results.get(0).getReviewedBy());
    }

    @Test
    void getGradingResultBySubmissionId_shouldReturnResult() {
        when(gradingResultMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(pendingResult);

        GradingResult result = gradingService.getGradingResultBySubmissionId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSubmissionId());
    }
}
