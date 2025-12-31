package com.almousleck.service;

import com.almousleck.model.SystemLog;
import com.almousleck.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SystemLogService {
    SystemLog logAction(String action, User user, String resourceType, Long resourceId,
                        String oldValue, String newValue, HttpServletRequest request);
    SystemLog logAction(String action, User user, HttpServletRequest request);
    Page<SystemLog> getLogs(Pageable pageable);
    Page<SystemLog> getLogsByUser(Long userId, Pageable pageable);
    Page<SystemLog> getLogsByAction(String action, Pageable pageable);
    Page<SystemLog> getLogsByResource(String resourceType, Long resourceId, Pageable pageable);
    Page<SystemLog> getLogsByDateRange(Instant start, Instant end, Pageable pageable);
}

