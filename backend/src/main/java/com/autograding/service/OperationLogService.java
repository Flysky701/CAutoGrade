package com.autograding.service;

import com.autograding.entity.OperationLog;
import com.autograding.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void logOperation(Long userId, String action, String targetType, Long targetId,
                           String detail, String ipAddress) {
        try {
            OperationLog opLog = new OperationLog();
            opLog.setUserId(userId);
            opLog.setAction(action);
            opLog.setTargetType(targetType);
            opLog.setTargetId(targetId);
            opLog.setDetail(detail);
            opLog.setIpAddress(ipAddress != null ? ipAddress : resolveClientIp());
            opLog.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            log.warn("Failed to log operation: action={}, detail={}, error={}", action, detail, e.getMessage());
        }
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) {
                    return ip.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
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
