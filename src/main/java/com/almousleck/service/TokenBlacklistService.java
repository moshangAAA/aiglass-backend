package com.almousleck.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "JWT_BLACKLIST:";

    public void blacklistToken(String token, long expirationInSeconds) {
        // Store in Redis with the exact same TTL
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "revoked",
                expirationInSeconds,
                TimeUnit.SECONDS
        );
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
    }
}
