package com.autograding.service;

import com.autograding.entity.OperationLog;
import com.autograding.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void logOperation(Long userId, String action, String targetType, Long targetId,
                           String detail, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public List<OperationLog> getRecentLogs(int limit) {
        return operationLogMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt)
                .last("limit " + limit)
        );
    }

    public List<OperationLog> getLogsByUser(Long userId, int limit) {
        return operationLogMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getUserId, userId)
                .orderByDesc(OperationLog::getCreatedAt)
                .last("limit " + limit)
        );
    }
}
