package com.almousleck.config.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.sms")
public class AliyunSmsConfig {
    private boolean enabled;
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String regionId;
    private String endpoint;
    private Map<String, String> template;
}
