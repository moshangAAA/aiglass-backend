package com.almousleck.model;

import com.almousleck.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(
        name = "system_logs",
        indexes = {
                @Index(name = "idx_system_logs_user", columnList = "user_id"),
                @Index(name = "idx_system_logs_action", columnList = "action"),
                @Index(name = "idx_system_logs_created", columnList = "created_at")
        }
)
public class SystemLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String action; // e.g., "USER_LOGIN", "USER_BANNED", "DEVICE_PAIRED"

    @Column(name = "resource_type", length = 50)
    private String resourceType; // e.g., "USER", "DEVICE", "SESSION"

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue; // JSON string

    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue; // JSON string

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}

