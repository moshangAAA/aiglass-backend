package com.almousleck.service;

import com.almousleck.dto.*;

public interface AuthenticationService {
    OtpResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void verifyOtp(String phoneNumber, String otpCode);
    OtpResponse resendOtp(String phoneNumber);
    OtpResponse forgotPassword(String phoneNumber);
    void resetPassword(String phoneNumber, String otpCode, String password);
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);
    void logout(String accessToken, String refreshToken);
}
