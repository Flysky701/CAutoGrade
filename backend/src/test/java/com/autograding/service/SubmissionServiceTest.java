package com.autograding.service;

import com.autograding.entity.Assignment;
import com.autograding.entity.GradingResult;
import com.autograding.entity.Submission;
import com.autograding.mapper.AssignmentMapper;
import com.autograding.mapper.GradingResultMapper;
import com.autograding.mapper.SubmissionMapper;
import com.autograding.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private GradingResultMapper gradingResultMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private OperationLogService operationLogService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private SubmissionService submissionService;

    private Assignment assignment;

    @BeforeEach
    void setUp() {
        assignment = new Assignment();
        assignment.setId(1L);
        assignment.setTitle("测试作业");
        assignment.setEndTime(LocalDateTime.now().plusDays(7));
        assignment.setDeleted(0);
    }

    @Test
    void submitCode_shouldCreateSubmissionAndPendingGrading() {
        when(assignmentMapper.selectById(1L)).thenReturn(assignment);
        when(submissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(submissionMapper.insert(any(Submission.class))).thenReturn(1);
        when(gradingResultMapper.insert(any(GradingResult.class))).thenReturn(1);

        Submission result = submissionService.submitCode(1L, 1L, 1L,
                "#include <stdio.h>\nint main() { return 0; }");

        assertNotNull(result);
        assertEquals(1L, result.getAssignmentId());
        assertEquals(1L, result.getProblemId());
        assertEquals(1L, result.getStudentId());
        assertEquals(1, result.getSubmitCount());

        ArgumentCaptor<Submission> subCap = ArgumentCaptor.forClass(Submission.class);
        verify(submissionMapper).insert(subCap.capture());
        assertEquals("c", subCap.getValue().getLanguage());

        ArgumentCaptor<GradingResult> grCap = ArgumentCaptor.forClass(GradingResult.class);
        verify(gradingResultMapper).insert(grCap.capture());
        assertEquals(GradingResult.GradingStatus.PENDING, grCap.getValue().getGradingStatus());
    }

    @Test
    void submitCode_shouldMarkLateWhenPastDeadline() {
        assignment.setEndTime(LocalDateTime.now().minusHours(1));
        when(assignmentMapper.selectById(1L)).thenReturn(assignment);
        when(submissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(submissionMapper.insert(any(Submission.class))).thenReturn(1);
        when(gradingResultMapper.insert(any(GradingResult.class))).thenReturn(1);

        Submission result = submissionService.submitCode(1L, 1L, 1L, "int main(){}");

        assertEquals(1, result.getIsLate());
    }

    @Test
    void reviewGrading_shouldUpdateScoreAndReviewer() {
        GradingResult existing = new GradingResult();
        existing.setId(10L);
        existing.setSubmissionId(1L);
        existing.setTotalScore(new BigDecimal("75.00"));
        existing.setGradingStatus(GradingResult.GradingStatus.DONE);
        when(gradingResultMapper.selectById(10L)).thenReturn(existing);
        when(gradingResultMapper.updateById(any(GradingResult.class))).thenReturn(1);

        GradingResult result = submissionService.reviewGrading(10L, 2L,
                new BigDecimal("80.00"), "批阅合理，微调分数");

        assertNotNull(result);
        assertEquals(new BigDecimal("80.00"), result.getHumanAdjustedScore());
        assertEquals(2L, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
    }
}
