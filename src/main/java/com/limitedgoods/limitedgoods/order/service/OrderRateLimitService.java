package com.limitedgoods.limitedgoods.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRateLimitService {

    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_SECONDS = 10;

    private static final String RATE_LIMIT_SCRIPT = """
            local count = redis.call('INCR', KEYS[1])

            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end

            if count > tonumber(ARGV[2]) then
                return 0
            end

            return 1
            """;

    private static final RedisScript<Long> SCRIPT =
            RedisScript.of(RATE_LIMIT_SCRIPT, Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean allow(Long userId, Long productId) {
        String key = "rate-limit:order:" + userId + ":" + productId;

        try {
            Long result = redisTemplate.execute(
                    SCRIPT,
                    List.of(key),
                    String.valueOf(WINDOW_SECONDS),
                    String.valueOf(MAX_REQUESTS)
            );

            return Long.valueOf(1L).equals(result);

        } catch (Exception e) {
            log.warn(
                    "주문 요청 제한 확인 실패. 요청을 통과시킵니다. userId={}, productId={}",
                    userId,
                    productId,
                    e
            );

            return true;
        }
    }
}