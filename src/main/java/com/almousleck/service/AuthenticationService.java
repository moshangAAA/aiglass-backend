package com.almousleck.service;

import com.almousleck.dto.AuthResponse;
import com.almousleck.dto.LoginRequest;
import com.almousleck.dto.OtpResponse;
import com.almousleck.dto.RegisterRequest;

public interface AuthenticationService {
    OtpResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void verifyOtp(String phoneNumber, String otpCode);
    OtpResponse resendOtp(String phoneNumber);
    OtpResponse forgotPassword(String phoneNumber);
    void resetPassword(String phoneNumber, String otpCode, String password);
}
