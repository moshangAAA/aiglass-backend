package com.almousleck.config.server;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
    private String endpoint;
    private String regionId;
    private Map<String, String> template;

    @Bean
    public Client aliyunSmsClient() throws Exception {
        if (!enabled) return null;

        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint)
                .setRegionId(regionId);

        return new Client(config);
    }
}
