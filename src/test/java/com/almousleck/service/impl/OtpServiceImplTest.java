package com.almousleck.service.impl;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    protected ValueOperations<String, String> valueOperations;
    @InjectMocks
    private OtpServiceImpl otpService;

    private final String phone = "13800138000";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "rateLimitSeconds", 60);
    }

    @Test
    @DisplayName("Should generate OTP and store in Redis with TTL")
    void generateOtp_Success() {
        String code = otpService.generateOtp(phone);
        assertNotNull(code);
        assertEquals(6, code.length());
        verify(valueOperations).set(eq("otp:code:" + phone), eq(code), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq("otp:limit:" + phone), anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should return true for valid code from Redis")
    void validateOtp_Valid() {
        when(valueOperations.get("otp:code:" + phone)).thenReturn("123456");
        assertTrue(otpService.validateOtp(phone, "123456"));
    }

    @Test
    @DisplayName("Should check rate limit presence in Redis")
    void isRateLimited_True() {
        when(redisTemplate.hasKey("otp:limit:" + phone)).thenReturn(true);
        assertTrue(otpService.isRateLimited(phone));
    }
}