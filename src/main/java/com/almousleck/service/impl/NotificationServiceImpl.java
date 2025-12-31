package com.almousleck.service.impl;

import com.almousleck.exceptions.SmsException;
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

    private final AliyunSmsService aliyunSmsService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        executeSafe(() -> aliyunSmsService.sendOtp(phoneNumber, otpCode), "OTP", phoneNumber, "Code: " + otpCode);
    }

    @Override
    public void sendAccountLockedNotification(String phoneNumber, LocalDateTime unlockTime) {
        String formattedTime = unlockTime.format(TIME_FORMATTER);
        executeSafe(() -> aliyunSmsService.sendAccountLocked(phoneNumber, formattedTime), "ACC_LOCKED", phoneNumber, "Unlock at: " + formattedTime);
    }

    @Override
    public void sendPhoneVerifiedNotification(String phoneNumber) {
        executeSafe(() -> aliyunSmsService.sendVerificationSuccess(phoneNumber), "PHONE_VERIFIED", phoneNumber, "Verification Success");
    }

    @Override
    public void sendPasswordResetOtp(String phoneNumber, String otpCode, int expiryMinutes) {
        executeSafe(() -> aliyunSmsService.sendPasswordResetOtp(phoneNumber, otpCode), "PWD_RESET_OTP", phoneNumber, "Reset Code: " + otpCode);
    }

    @Override
    public void sendPasswordResetConfirmation(String phoneNumber) {
        executeSafe(() -> aliyunSmsService.sendPasswordChanged(phoneNumber), "PWD_CHANGED", phoneNumber, "Password Updated Successfully");
    }

    @Override
    public void sendLoginWarningNotification(String phoneNumber, int attemptsRemaining) {
        executeSafe(() -> aliyunSmsService.sendLoginWarning(phoneNumber, attemptsRemaining), "LOGIN_WARN", phoneNumber, "Attempts left: " + attemptsRemaining);
    }

    /**
     *Our helper methods do: Executes an SMS action safely.
     * If the SMS provider fails, it logs the error and provides a fallback console log
     * to ensure business continuity during development or provider outages.
     */
    private void executeSafe(Runnable action, String type, String phone, String context) {
        try {
            action.run();
        }  catch (SmsException ex) {
            log.error("Notification provider failed for {} ({}). Error: {}", phone, type, ex.getMessage());
            logFallback(type, phone, context);
        } catch (Exception ex) {
            log.error("Unexpected error in notification flow for {}", phone, ex);
            logFallback(type, phone, context);
        }
    }

    private void logFallback(String type, String phone, String context) {
        log.info("================ FALLBACK NOTIFICATION ================");
        log.info("TYPE    : {}", type);
        log.info("TO      : {}", phone);
        log.info("CONTENT : {}", context);
        log.info("PROD TIP: Ensure ALIYUN_SMS_ENABLED is true and keys are valid.");
        log.info("=======================================================");
    }
}
