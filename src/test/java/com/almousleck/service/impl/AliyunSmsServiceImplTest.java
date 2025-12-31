package com.almousleck.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.almousleck.config.server.AliyunSmsConfig;
import com.almousleck.exceptions.SmsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AliyunSmsServiceImplTest {

    @Mock
    private AliyunSmsConfig smsConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Client aliyunClient;

    @InjectMocks
    private AliyunSmsServiceImpl aliyunSmsService;

    private final String phone = "13800138000";

    @BeforeEach
    void setUp() {
        Map<String, String> templates = new HashMap<>();
        templates.put("otp", "SMS_123");
        lenient().when(smsConfig.getTemplate()).thenReturn(templates);
        lenient().when(smsConfig.getSignName()).thenReturn("AI Glass");
    }

    @Test
    @DisplayName("Should skip sending if SMS is disabled")
    void sendSms_WhenDisabled_ShouldReturnEarly() {
        when(smsConfig.isEnabled()).thenReturn(false);

        aliyunSmsService.sendOtp(phone, "1234");

        verifyNoInteractions(aliyunClient);
    }

    @Test
    @DisplayName("Should throw SmsException when Aliyun returns non-OK code")
    void sendSms_WhenAliyunReturnsError_ShouldThrowException() throws Exception {
        when(smsConfig.isEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(aliyunSmsService, "aliyunClient", aliyunClient);

        SendSmsResponse mockResponse = mock(SendSmsResponse.class);
        SendSmsResponseBody mockBody = mock(SendSmsResponseBody.class);
        when(mockBody.getCode()).thenReturn("isv.BUSINESS_LIMIT_CONTROL");
        when(mockBody.getMessage()).thenReturn("Too frequent");
        when(mockResponse.getBody()).thenReturn(mockBody);

        when(aliyunClient.sendSms(any(SendSmsRequest.class))).thenReturn(mockResponse);

        assertThrows(SmsException.class, () -> aliyunSmsService.sendOtp(phone, "1234"));
    }

    @Test
    @DisplayName("Should successfully call Aliyun Client when enabled")
    void sendSms_Success() throws Exception {
        when(smsConfig.isEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(aliyunSmsService, "aliyunClient", aliyunClient);

        SendSmsResponse mockResponse = mock(SendSmsResponse.class);
        SendSmsResponseBody mockBody = mock(SendSmsResponseBody.class);
        when(mockBody.getCode()).thenReturn("OK");
        when(mockResponse.getBody()).thenReturn(mockBody);

        when(aliyunClient.sendSms(any(SendSmsRequest.class))).thenReturn(mockResponse);

        aliyunSmsService.sendOtp(phone, "1234");

        verify(aliyunClient, times(1)).sendSms(any(SendSmsRequest.class));
    }
}