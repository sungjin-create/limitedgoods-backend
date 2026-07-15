package com.limitedgoods.limitedgoods.queue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionTokenService {

    private static final String TOKEN_PREFIX  = "admission:token:";
    private static final String TRACK_PREFIX  = "admission:track:";
    private static final Duration TOKEN_TTL   = Duration.ofSeconds(300);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 입장 토큰 발급
     * 이미 발급된 토큰이 있으면 기존 토큰 재사용 (중복 발급 방지)
     */
    public String issueToken(Long userId, Long productId) {
        String trackKey = TRACK_PREFIX + userId + ":" + productId;

        String existingUuid = redisTemplate.opsForValue().get(trackKey);
        if (existingUuid != null) {
            return existingUuid;
        }

        String uuid       = UUID.randomUUID().toString();
        String tokenKey   = TOKEN_PREFIX + uuid;
        String tokenValue = userId + ":" + productId;

        redisTemplate.opsForValue().set(tokenKey, tokenValue, TOKEN_TTL);
        redisTemplate.opsForValue().set(trackKey, uuid, TOKEN_TTL);

        return uuid;
    }

    /**
     * 토큰 검증 및 소비 (1회용)
     * 검증 성공 시 Redis에서 즉시 삭제
     */
    public boolean validateAndConsume(String token, Long userId, Long productId) {
        String tokenKey = TOKEN_PREFIX + token;
        String value    = redisTemplate.opsForValue().get(tokenKey);

        if (value == null) {
            return false;
        }

        String expected = userId + ":" + productId;
        if (!expected.equals(value)) {
            return false;
        }

        redisTemplate.delete(tokenKey);
        redisTemplate.delete(TRACK_PREFIX + userId + ":" + productId);

        return true;
    }
}