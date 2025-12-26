package com.almousleck.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${app.ratelimit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Bean
    public RedisClient redisClient() {
        RedisURI redisURI = RedisURI.Builder
                .redis(redisHost, redisPort)
                .build();
        return RedisClient.create(redisURI);
    }

    @Bean
    public LettuceBasedProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient).build();
    }

    @Bean
    public BucketConfiguration bucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        requestsPerMinute,
                        Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
                ))
                .build();
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }
}