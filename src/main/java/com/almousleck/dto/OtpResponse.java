package com.almousleck.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OTP operations (registration, password reset, etc.)
 * 
 * Note: The otpCode field is only included in development mode for testing purposes.
 * In production, OTP codes are sent via SMS and not included in the response.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpResponse {
    private String message;
    private int expiresInSeconds;
    
    /**
     * OTP code (only populated in development mode when app.security.otp.include-in-response=true).
     * In production, this will always be null as OTPs are sent via SMS.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String otpCode;
}
