package com.almousleck.model;

import com.almousleck.common.BaseEntity;
import com.almousleck.enums.UserRole;
import com.almousleck.enums.UserStatus;
import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_phone", columnList = "phone_number")
        }
)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "phone_number", nullable = false, unique = true, length = 255)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private Integer usageCount = 0; // Number of navigations

    @Column(nullable = false)
    private Integer totalDistance = 0; // total distance from zero

    @Column(columnDefinition = "JSON")
    private String preference; // JSON to store preferences Ex: "voice": "female" "lg": "ch"

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 6)
    private String otpCode;

    private LocalDateTime otpGeneratedAt;

    @Column(nullable = false)
    private Boolean otpVerified = false;

    @Column(nullable = false)
    private Boolean phoneVerified = false;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    private LocalDateTime lockoutTime;

    @Column(nullable = false)
    private Boolean locked = false;
}


