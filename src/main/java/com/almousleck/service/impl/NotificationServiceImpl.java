package com.almousleck.service.impl;

import com.almousleck.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Value("${notification.mode:console}")
    private String notificationMode;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        String message = String.format(
                "【AI Glass】您的验证码是：%s，有效期%d分钟，请勿泄露给他人。",
                otpCode, expiryMinutes
        );
        sendNotification("OTP", phoneNumber, message);
    }

    @Override
    public void sendAccountLockedNotification(String phoneNumber, LocalDateTime unlockTime) {
        String message = String.format(
                "【AI Glass】您的账户因多次登录失败已被锁定，解锁时间：%s。如非本人操作，请立即联系客服。",
                unlockTime.format(TIME_FORMATTER)
        );
        sendNotification("SECURITY ALERT", phoneNumber, message);
    }

    @Override
    public void sendPhoneVerifiedNotification(String phoneNumber) {
        String message = "【AI Glass】您的手机号已成功验证，欢迎使用AI Glass服务！";
        sendNotification("VERIFICATION", phoneNumber, message);
    }

    @Override
    public void sendPasswordResetOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        String message = String.format(
                "【AI Glass】您正在重置密码，验证码是：%s，有效期%d分钟。如非本人操作，请忽略此消息。",
                otpCode, expiryMinutes
        );
        sendNotification("PASSWORD RESET", phoneNumber, message);
    }

    @Override
    public void sendPasswordResetConfirmation(String phoneNumber) {
        String message = "【AI Glass】您的密码已成功重置，请使用新密码登录。如非本人操作，请立即联系客服。";
        sendNotification("PASSWORD CHANGED", phoneNumber, message);
    }

    @Override
    public void sendLoginWarningNotification(String phoneNumber, int attemptsRemaining) {
        String message = String.format(
                "【AI Glass】安全警告：您的账户登录失败次数过多，还剩 %d 次尝试机会。再次失败将锁定账户30分钟。",
                attemptsRemaining
        );
        sendNotification("LOGIN WARNING", phoneNumber, message);
    }

    // Helper method to send notifications

    private void sendNotification(String type, String phoneNumber, String message) {
        if ("console".equalsIgnoreCase(notificationMode)) {
            log.info("=".repeat(60));
            log.info("📱 SMS [{}] to {}", type, phoneNumber);
            log.info("📄 Message: {}", message);
            log.info("=".repeat(60));
        } else {
            sendSms(phoneNumber, message);
        }
    }

    private void sendSms(String phoneNumber, String message) {
        // TODO: Implement actual SMS sending
        log.info("SMS sent to {}: {}", phoneNumber, message);
    }
}
