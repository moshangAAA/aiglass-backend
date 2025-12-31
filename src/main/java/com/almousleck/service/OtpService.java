package com.almousleck.service;

public interface OtpService {
    String generateOtp(String key);
    boolean validateOtp(String key, String code);
    boolean isRateLimited(String key);
    void clearOtp(String key);
}
