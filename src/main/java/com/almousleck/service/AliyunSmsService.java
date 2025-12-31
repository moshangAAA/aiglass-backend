package com.almousleck.service;

public interface AliyunSmsService {
    void sendOtp(String phone, String code);
    void sendPasswordResetOtp(String phone, String code);
    void sendLoginWarning(String phone, int remaining);
    void sendAccountLocked(String phone, String unlockTime);
    void sendVerificationSuccess(String phone);
    void sendPasswordChanged(String phone);
}
