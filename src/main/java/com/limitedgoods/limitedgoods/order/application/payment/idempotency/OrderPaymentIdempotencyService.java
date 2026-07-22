package com.limitedgoods.limitedgoods.order.application.payment.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponse;
import com.limitedgoods.limitedgoods.order.infrastructure.redis.PaymentRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OrderPaymentIdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long TTL_SECONDS = 3600;

    public OrderResponse getSavedResponse(Long userId, Long orderId, String idempotencyKey) {
        String key = PaymentRedisKeys.lock(userId, orderId, idempotencyKey);

        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, OrderResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("멱등 응답 역직렬화 실패", e);
        }
    }

    public boolean acquireLock(Long userId, Long orderId, String idempotencyKey) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                PaymentRedisKeys.lock(userId, orderId, idempotencyKey),
                "processing",
                Duration.ofSeconds(30)
        );
        return Boolean.TRUE.equals(success);
    }

    public void saveResponse(Long userId, Long orderId, String idempotencyKey, OrderResponse response) {
        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    PaymentRedisKeys.lock(userId, orderId, idempotencyKey),
                    value,
                    Duration.ofSeconds(TTL_SECONDS)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("멱등 응답 직렬화 실패", e);
        }
    }

    public void releaseLock(Long userId, Long orderId, String idempotencyKey) {
        redisTemplate.delete(PaymentRedisKeys.lock(userId, orderId, idempotencyKey));
    }
}
