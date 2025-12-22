package com.almousleck.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class NotificationService {
    @Value("${notification.mode:console}")
    private String notificationMode;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        String message = String.format(
                "【AI Glass】您的验证码是：%s，有效期%d分钟，请勿泄露给他人。",
                otpCode, expiryMinutes
        );
        sendNotification("OTP", phoneNumber, message);
    }

    public void sendAccountLockedNotification(String phoneNumber, LocalDateTime unlockTime) {
        String message = String.format(
                "【AI Glass】您的账户因多次登录失败已被锁定，解锁时间：%s。如非本人操作，请立即联系客服。",
                unlockTime.format(TIME_FORMATTER)
        );
        sendNotification("SECURITY ALERT", phoneNumber, message);
    }

    public void sendPhoneVerification(String phoneNumber) {
        String message = "【AI Glass】您的手机号已成功验证，欢迎使用AI Glass服务！";
        sendNotification("VERIFICATION", phoneNumber, message);
    }

    // Helpers methods
    private void sendNotification(String type, String phoneNumber, String message) {
        if ("console".equalsIgnoreCase(notificationMode)) {
            log.info("=".repeat(60));
            log.info("📱 SMS [{}] to {}", type, phoneNumber);
            log.info("📄 Message: {}", message);
            log.info("=".repeat(60));
        } else {
            // TODO: Integrate real SMS gateway (Aliyun, Twilio, etc.)
            sendSms(phoneNumber, message);
        }
    }

    private void sendSms(String phoneNumber, String message) {
        // TODO: Implement SMS sending logic
        log.info("Sending SMS to {}: {}", phoneNumber, message);
    }
}
