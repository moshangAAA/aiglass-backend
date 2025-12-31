package com.almousleck.service.impl;

import com.almousleck.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.otp-expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.security.otp-rate-limit-seconds:60}")
    private int rateLimitSeconds;

    private static final String OTP_PREFIX = "otp:code:";
    private static final String LIMIT_PREFIX = "otp:limit:";

    @Override
    public String generateOtp(String key) {
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        redisTemplate.opsForValue().set(OTP_PREFIX + key, code, otpExpiryMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(LIMIT_PREFIX + key, "locked", rateLimitSeconds, TimeUnit.SECONDS);
        return code;
    }

    @Override
    public boolean validateOtp(String key, String code) {
        String stored = redisTemplate.opsForValue().get(OTP_PREFIX + key);
        return code != null && code.equals(stored);
    }

    @Override
    public boolean isRateLimited(String key) {
        return redisTemplate.hasKey(LIMIT_PREFIX + key);
    }

    @Override
    public void clearOtp(String key) {
        redisTemplate.delete(OTP_PREFIX + key);
    }
}
