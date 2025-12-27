package com.almousleck.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.almousleck.config.server.AliyunSmsConfig;
import com.almousleck.exceptions.SmsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunSmsService {
    private final AliyunSmsConfig smsConfig;
    private Client client;

    public void sendOtp(String phone, String code) {
        sendSms(phone, smsConfig.getTemplate().get("otp"), Map.of("code", code));
    }

    public void sendPasswordResetOtp(String phone, String code) {
        sendSms(phone, smsConfig.getTemplate().get("password-reset"), Map.of("code", code));
    }

    public void sendLoginWarning(String phone, int remaining) {
        sendSms(phone, smsConfig.getTemplate().get("login-warning"),
                Map.of("attempts", String.valueOf(remaining)));
    }

    public void sendAccountLocked(String phone, String unlockTime) {
        sendSms(phone, smsConfig.getTemplate().get("account-locked"),
                Map.of("time", unlockTime));
    }

    public void sendVerificationSuccess(String phone) {
        sendSms(phone, smsConfig.getTemplate().get("verification-success"), Map.of());
    }

    public void sendPasswordChanged(String phone) {
        sendSms(phone, smsConfig.getTemplate().get("password-changed"), Map.of());
    }

    // Helps methods
    private Client getClient() {
        if (client == null) {
            try {
                Config config = new Config()
                        .setAccessKeyId(smsConfig.getAccessKeyId())
                        .setAccessKeySecret(smsConfig.getAccessKeySecret())
                        .setEndpoint(smsConfig.getEndpoint());
                client = new Client(config);
            } catch (Exception ex) {
                throw new SmsException("Failed to init SMS client", ex);
            }
        }
        return client;
    }

    private void sendSms(String phone, String templateCode, Map<String, String> params) {
        if (!smsConfig.isEnabled()) return;

        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(smsConfig.getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(toJson(params));

            SendSmsResponse response = getClient().sendSms(request);

            if (!"OK".equals(response.getBody().getCode()))
                throw new SmsException("SMS failed: " + response.getBody().getMessage());
        } catch (Exception ex) {
            log.error("SMS error for {}", params, ex);
            throw new SmsException("SMS failed: ", ex);
        }
    }

    private String toJson(Map<String, String> map) {
        if (map.isEmpty()) return "{}";
        StringBuilder stringBuilder = new StringBuilder("{");
        map.forEach((key, value) -> {
            if (stringBuilder.length() > 1) stringBuilder.append(",");
            stringBuilder.append("\"").append(key)
                    .append("\":\"").append(value).append("\"");
        });
        return stringBuilder.append("}").toString();
    }
}
