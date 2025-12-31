package com.almousleck.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.almousleck.config.server.AliyunSmsConfig;
import com.almousleck.exceptions.SmsException;
import com.almousleck.service.AliyunSmsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsServiceImpl implements AliyunSmsService {
    private final AliyunSmsConfig smsConfig;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private Client aliyunClient;

    @Override
    public void sendOtp(String phone, String code) {
        sendSms(phone, smsConfig.getTemplate().get("otp"), Map.of("code", code));
    }

    @Override
    public void sendPasswordResetOtp(String phone, String code) {
        sendSms(phone, smsConfig.getTemplate().get("password-reset"), Map.of("code", code));
    }

    @Override
    public void sendLoginWarning(String phone, int remaining) {
        sendSms(phone, smsConfig.getTemplate().get("login-warning"),
                Map.of("attempts", String.valueOf(remaining)));
    }

    @Override
    public void sendAccountLocked(String phone, String unlockTime) {
        sendSms(phone, smsConfig.getTemplate().get("account-locked"),
                Map.of("time", unlockTime));
    }

    @Override
    public void sendVerificationSuccess(String phone) {
        sendSms(phone, smsConfig.getTemplate().get("verification-success"), Map.of());
    }

    @Override
    public void sendPasswordChanged(String phone) {
        sendSms(phone, smsConfig.getTemplate().get("password-changed"), Map.of());
    }

    // Helpers methods
    private void sendSms(String phone, String templateCode, Map<String, String> params) {
        if (!smsConfig.isEnabled() || aliyunClient == null) {
            log.info("SMS delivery skipped for {}: Service is disabled via configuration.", phone);
            return;
        }

        try {
            String jsonParams = toJson(params);
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsConfig.getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(jsonParams);

            log.debug("Dispatching Aliyun SMS to {} [Template: {}]", phone, templateCode);
            SendSmsResponse response = aliyunClient.sendSms(request);

            if (response.getBody() == null || !"OK".equals(response.getBody().getCode())) {
                String errorCode = (response.getBody() != null) ? response.getBody().getCode() : "NULL_RESPONSE";
                String errorMessage = (response.getBody() != null) ? response.getBody().getMessage() : "No response body from Aliyun";

                log.error("Aliyun SMS failed for {}. Code: {}, Msg: {}", phone, errorCode, errorMessage);
                throw new SmsException("Aliyun API error: " + errorMessage);
            }

            log.info("SMS sent successfully to {}. RequestID: {}", phone, response.getBody().getRequestId());

        } catch (SmsException e) {
            throw e; // Pass through business-related SMS exceptions
        } catch (Exception ex) {
            log.error("Technical failure while communicating with Aliyun SMS API for {}", phone, ex);
            throw new SmsException("Internal SMS gateway error", ex);
        }
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SMS parameters to JSON", e);
            throw new SmsException("SMS parameter serialization failed", e);
        }
    }
}
