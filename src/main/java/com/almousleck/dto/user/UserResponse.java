package com.almousleck.dto.user;

import com.almousleck.enums.UserRole;
import com.almousleck.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private Integer usageCount;
    private Integer totalDistance;
    private String preference;
    private Boolean phoneVerified;
    private Boolean locked;
    private Instant created;
    private Instant updated;
}

