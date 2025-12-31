package com.almousleck.service.impl;

import com.almousleck.exceptions.SmsException;
import com.almousleck.service.AliyunSmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private AliyunSmsService aliyunSmsService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private final String phoneNumber = "13800138000";
    private final String otpCode = "123456";

    @Test
    @DisplayName("Should successfully call AliyunSmsService for OTP")
    void sendOtp_ShouldCallAliyunService() {
        notificationService.sendOtp(phoneNumber, otpCode, 5);
        verify(aliyunSmsService, times(1)).sendOtp(phoneNumber, otpCode);
    }

    @Test
    @DisplayName("Should catch SmsException and not rethrow to caller")
    void sendOtp_WhenSmsFails_ShouldHandleGracefully() {
        doThrow(new SmsException("API Error")).when(aliyunSmsService).sendOtp(anyString(), anyString());

        // Should not throw exception
        notificationService.sendOtp(phoneNumber, otpCode, 5);

        verify(aliyunSmsService).sendOtp(phoneNumber, otpCode);
    }

    @Test
    @DisplayName("Should call AliyunSmsService for account lock notification")
    void sendAccountLockedNotification_ShouldCallService() {
        LocalDateTime unlockTime = LocalDateTime.now().plusHours(1);
        notificationService.sendAccountLockedNotification(phoneNumber, unlockTime);

        verify(aliyunSmsService, times(1)).sendAccountLocked(eq(phoneNumber), anyString());
    }

    @Test
    @DisplayName("Should handle generic RuntimeException safely")
    void sendPasswordResetConfirmation_WhenUnexpectedError_ShouldNotFail() {
        doThrow(new RuntimeException("Crash")).when(aliyunSmsService).sendPasswordChanged(anyString());

        notificationService.sendPasswordResetConfirmation(phoneNumber);

        verify(aliyunSmsService).sendPasswordChanged(phoneNumber);
    }
}