package com.almousleck.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpResponse {
    private String message;
    private int expiresInSeconds;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    //TODO: Only for dev mode / Implement SMS OTP for production
    private String otpCode;
}
