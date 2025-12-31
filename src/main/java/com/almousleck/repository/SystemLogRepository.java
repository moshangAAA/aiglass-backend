package com.almousleck.repository;

import com.almousleck.model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    Page<SystemLog> findByUserId(Long userId, Pageable pageable);
    Page<SystemLog> findByAction(String action, Pageable pageable);
    Page<SystemLog> findByResourceTypeAndResourceId(String resourceType, Long resourceId, Pageable pageable);
    Page<SystemLog> findByCreatedBetween(Instant start, Instant end, Pageable pageable);
}

