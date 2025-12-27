package com.almousleck.service.impl;

import com.almousleck.service.AliyunSmsService;
import com.almousleck.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final AliyunSmsService smsService;

    @Value("${aliyun.sms.enabled:false}")
    private boolean smsEnabled;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        String message = String.format("验证码: %s (有效期%d分钟)", otpCode, expiryMinutes);
        if (smsEnabled)
            smsService.sendOtp(phoneNumber, otpCode);
        else
            logSms("OTP验证码", phoneNumber, message);
    }

    @Override
    public void sendAccountLockedNotification(String phoneNumber, LocalDateTime unlockTime) {
        String formattedTime = unlockTime.format(TIME_FORMATTER);
        String message = String.format("账户已被锁定，解锁时间: %s", formattedTime);
        if (smsEnabled)
            smsService.sendAccountLocked(phoneNumber, formattedTime);
        else
            logSms("账户锁定", phoneNumber, message);
    }

    @Override
    public void sendPhoneVerifiedNotification(String phoneNumber) {
        String message = "手机号验证成功";
        if (smsEnabled)
            smsService.sendVerificationSuccess(phoneNumber);
        else
            logSms("验证成功", phoneNumber, message);
    }

    @Override
    public void sendPasswordResetOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        String message = String.format("密码重置验证码: %s (有效期%d分钟)", otpCode, expiryMinutes);
        if (smsEnabled)
            smsService.sendPasswordResetOtp(phoneNumber, otpCode);
        else
            logSms("密码重置", phoneNumber, message);
    }

    @Override
    public void sendPasswordResetConfirmation(String phoneNumber) {
        String message = "密码已成功修改";
        if (smsEnabled)
            smsService.sendPasswordChanged(phoneNumber);
        else
            logSms("密码修改", phoneNumber, message);
    }

    @Override
    public void sendLoginWarningNotification(String phoneNumber, int attemptsRemaining) {
        String message = String.format("登录失败警告，剩余尝试次数: %d", attemptsRemaining);
        if (smsEnabled) {
            smsService.sendLoginWarning(phoneNumber, attemptsRemaining);
        } else {
            logSms("登录警告", phoneNumber, message);
        }
    }

    // Helper method to send notifications
    private void logSms(String type, String phoneNumber, String message) {
        log.info("=".repeat(60));
        log.info("📱 SMS [{}] -> {}", type, phoneNumber);
        log.info("📄 内容: {}", message);
        log.info("💡 提示: 生产环境启用 ALIYUN_SMS_ENABLED=true");
        log.info("=".repeat(60));
    }
}
