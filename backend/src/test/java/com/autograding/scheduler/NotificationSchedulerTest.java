package com.autograding.scheduler;

import com.autograding.entity.*;
import com.autograding.mapper.*;
import com.autograding.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock private AssignmentMapper assignmentMapper;
    @Mock private ClassMapper classMapper;
    @Mock private ClassStudentMapper classStudentMapper;
    @Mock private SubmissionMapper submissionMapper;
    @Mock private GradingResultMapper gradingResultMapper;
    @Mock private NotificationService notificationService;
    @Mock private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private NotificationScheduler scheduler;

    private GradingResult processingResult;
    private GradingResult pendingStuckResult;

    @BeforeEach
    void setUp() {
        processingResult = new GradingResult();
        processingResult.setId(1L);
        processingResult.setSubmissionId(10L);
        processingResult.setGradingStatus(GradingResult.GradingStatus.PROCESSING);
        processingResult.setGradedAt(LocalDateTime.now().minusMinutes(45));

        pendingStuckResult = new GradingResult();
        pendingStuckResult.setId(2L);
        pendingStuckResult.setSubmissionId(11L);
        pendingStuckResult.setGradingStatus(GradingResult.GradingStatus.PENDING);
        pendingStuckResult.setGradedAt(LocalDateTime.now().minusMinutes(45));
    }

    @Test
    void checkStuckGradingTasks_resetsProcessingToPending() {
        when(gradingResultMapper.selectList(argThat(wrapper -> {
            return true;
        }))).thenReturn(List.of(processingResult)).thenReturn(List.of());
        when(gradingResultMapper.updateById(any(GradingResult.class))).thenReturn(1);

        scheduler.checkStuckGradingTasks();

        verify(gradingResultMapper).updateById(argThat(gr ->
                gr.getId().equals(1L) &&
                gr.getGradingStatus() == GradingResult.GradingStatus.PENDING
        ));
    }

    @Test
    void checkStuckGradingTasks_logsStuckPendingCount() {
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(pendingStuckResult));

        scheduler.checkStuckGradingTasks();

        verify(gradingResultMapper, never()).updateById(argThat(gr ->
                gr.getId().equals(2L)
        ));
    }

    @Test
    void checkStuckGradingTasks_noStuckTasks_noAction() {
        when(gradingResultMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of())
                .thenReturn(List.of());

        scheduler.checkStuckGradingTasks();

        verify(gradingResultMapper, never()).updateById(any(GradingResult.class));
    }
}
