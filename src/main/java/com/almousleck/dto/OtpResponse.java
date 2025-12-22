package com.almousleck.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // ignore null fields
public class OtpResponse {
    private String message;
    private int expiresInSeconds;
    private String otpCode; //TODO: Only for dev mode / Implement SMS OTP for production
}
