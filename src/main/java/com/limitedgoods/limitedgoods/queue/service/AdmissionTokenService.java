package com.limitedgoods.limitedgoods.queue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdmissionTokenService {

    private static final String TOKEN_PREFIX = "admission:token:";
    private static final String TRACK_PREFIX = "admission:track:";
    private static final Duration TOKEN_TTL = Duration.ofSeconds(300);

    private static final RedisScript<Long> CLAIM_SCRIPT = RedisScript.of(
            """
            local tokenValue = redis.call('GET', KEYS[1])

            if not tokenValue then
                return 0
            end

            if tokenValue == ARGV[2] then
                return 1
            end

            if tokenValue ~= ARGV[1] then
                return 0
            end

            local ttl = redis.call('PTTL', KEYS[1])

            if ttl <= 0 then
                return 0
            end

            redis.call('SET', KEYS[1], ARGV[2], 'PX', ttl)
            return 1
            """,
            Long.class
    );

    private static final RedisScript<Long> COMPLETE_CONSUMPTION_SCRIPT = RedisScript.of(
            """
            local tokenValue = redis.call('GET', KEYS[1])

            if tokenValue ~= ARGV[1] then
                return 0
            end

            redis.call('DEL', KEYS[1])

            local trackedToken = redis.call('GET', KEYS[2])

            if trackedToken == ARGV[2] then
                redis.call('DEL', KEYS[2])
            end

            return 1
            """,
            Long.class
    );

    private static final RedisScript<Long> RELEASE_CLAIM_SCRIPT = RedisScript.of(
            """
            local tokenValue = redis.call('GET', KEYS[1])

            if tokenValue ~= ARGV[1] then
                return 0
            end

            local ttl = redis.call('PTTL', KEYS[1])

            if ttl <= 0 then
                return 0
            end

            redis.call('SET', KEYS[1], ARGV[2], 'PX', ttl)
            return 1
            """,
            Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;

    public String issueToken(Long userId, Long productId) {
        String trackKey = TRACK_PREFIX + userId + ":" + productId;

        String existingUuid = redisTemplate.opsForValue().get(trackKey);
        if (existingUuid != null) {
            return existingUuid;
        }

        String uuid = UUID.randomUUID().toString();
        String tokenKey = TOKEN_PREFIX + uuid;

        redisTemplate.opsForValue().set(tokenKey, tokenValue(userId, productId), TOKEN_TTL);
        redisTemplate.opsForValue().set(trackKey, uuid, TOKEN_TTL);

        return uuid;
    }

    public boolean claim(String token, Long userId, Long productId, String checkoutToken) {
        Long result = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(TOKEN_PREFIX + token),
                tokenValue(userId, productId),
                processingValue(userId, productId, checkoutToken)
        );

        return Long.valueOf(1L).equals(result);
    }

    public boolean completeConsumption(String token, Long userId, Long productId, String checkoutToken) {
        Long result = redisTemplate.execute(
                COMPLETE_CONSUMPTION_SCRIPT,
                List.of(
                        TOKEN_PREFIX + token,
                        TRACK_PREFIX + userId + ":" + productId
                ),
                processingValue(userId, productId, checkoutToken),
                token
        );

        return Long.valueOf(1L).equals(result);
    }

    public boolean releaseClaim(String token, Long userId, Long productId, String checkoutToken) {
        Long result = redisTemplate.execute(
                RELEASE_CLAIM_SCRIPT,
                List.of(TOKEN_PREFIX + token),
                processingValue(userId, productId, checkoutToken),
                tokenValue(userId, productId)
        );

        return Long.valueOf(1L).equals(result);
    }

    private String tokenValue(Long userId, Long productId) {
        return userId + ":" + productId;
    }

    private String processingValue(Long userId, Long productId, String checkoutToken) {
        return "PROCESSING:" + userId + ":" + productId + ":" + checkoutToken;
    }
}
