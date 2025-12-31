package com.almousleck.service.impl;

import com.almousleck.model.SystemLog;
import com.almousleck.model.User;
import com.almousleck.repository.SystemLogRepository;
import com.almousleck.service.SystemLogService;
import com.almousleck.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements SystemLogService {

    private final SystemLogRepository systemLogRepository;

    @Override
    public SystemLog logAction(String action, User user, String resourceType, Long resourceId, String oldValue, String newValue, HttpServletRequest request) {
        SystemLog logEntry = SystemLog.builder()
                .action(action)
                .user(user)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(HttpRequestUtil.getClientIp(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        return systemLogRepository.save(logEntry);
    }

    @Override
    public SystemLog logAction(String action, User user, HttpServletRequest request) {
        return logAction(action, user, null, null, null, null, request);
    }

    @Override
    public Page<SystemLog> getLogs(Pageable pageable) {
        return systemLogRepository.findAll(pageable);
    }

    @Override
    public Page<SystemLog> getLogsByUser(Long userId, Pageable pageable) {
        return systemLogRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<SystemLog> getLogsByAction(String action, Pageable pageable) {
        return systemLogRepository.findByAction(action, pageable);
    }

    @Override
    public Page<SystemLog> getLogsByResource(String resourceType, Long resourceId, Pageable pageable) {
        return systemLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId, pageable);
    }

    @Override
    public Page<SystemLog> getLogsByDateRange(Instant start, Instant end, Pageable pageable) {
        return systemLogRepository.findByCreatedBetween(start, end, pageable);
    }
}
