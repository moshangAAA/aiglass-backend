package com.almousleck.dto.user;

import com.almousleck.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "Status is required")
    private UserStatus status;
    private String reason; // Optional reason for status change
}

