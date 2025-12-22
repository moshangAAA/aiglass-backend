package com.almousleck.service;


import java.time.LocalDateTime;

public interface NotificationService {
    void sendOtp(String phoneNumber, String otpCode, int expiryMinutes);
    void sendAccountLockedNotification(String phoneNumber, LocalDateTime unlockTime);
    void sendPhoneVerifiedNotification(String phoneNumber);
    void sendPasswordResetOtp(String phoneNumber, String otpCode, int expiryMinutes);
    void sendPasswordResetConfirmation(String phoneNumber);
    void sendLoginWarningNotification(String phoneNumber, int attemptsRemaining);
}
