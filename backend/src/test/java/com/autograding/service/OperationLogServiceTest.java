package com.autograding.service;

import com.autograding.entity.OperationLog;
import com.autograding.mapper.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceTest {

    @Mock private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogService operationLogService;

    private OperationLog log;

    @BeforeEach
    void setUp() {
        log = new OperationLog();
        log.setId(1L);
        log.setUserId(10L);
        log.setAction("CREATE");
        log.setTargetType("COURSE");
        log.setTargetId(5L);
        log.setDetail("{\"name\": \"C语言\"}");
        log.setIpAddress("127.0.0.1");
        log.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void logOperation_shouldInsert() {
        when(operationLogMapper.insert(any(OperationLog.class))).thenReturn(1);

        operationLogService.logOperation(10L, "CREATE", "COURSE", 5L, "测试日志", "127.0.0.1");

        verify(operationLogMapper).insert(any(OperationLog.class));
    }

    @Test
    void getRecentLogs_shouldReturnList() {
        when(operationLogMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(log));

        List<OperationLog> results = operationLogService.getRecentLogs(50);

        assertEquals(1, results.size());
        assertEquals("CREATE", results.get(0).getAction());
    }

    @Test
    void getRecentLogs_shouldReturnEmptyList() {
        when(operationLogMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<OperationLog> results = operationLogService.getRecentLogs(50);

        assertTrue(results.isEmpty());
    }

    @Test
    void getLogsByUser_shouldReturnFilteredList() {
        when(operationLogMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(log));

        List<OperationLog> results = operationLogService.getLogsByUser(10L, 30);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getUserId());
    }

    @Test
    void getLogsByUser_shouldReturnEmptyWhenNoMatch() {
        when(operationLogMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<OperationLog> results = operationLogService.getLogsByUser(99L, 30);

        assertTrue(results.isEmpty());
    }
}
