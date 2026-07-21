package com.limitedgoods.limitedgoods.order.application.payment.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.order.dto.response.OrderResponseDto;
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

    public String key(Long userId, Long orderId, String idempotencyKey) {
        return "idem:pay:" + userId + ":" + orderId + ":" + idempotencyKey;
    }

    public OrderResponseDto getSavedResponse(Long userId, Long orderId, String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(key(userId, orderId, idempotencyKey));
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, OrderResponseDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("멱등 응답 역직렬화 실패", e);
        }
    }

    public boolean acquireLock(Long userId, Long orderId, String idempotencyKey) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                key(userId, orderId, idempotencyKey) + ":lock",
                "processing",
                Duration.ofSeconds(30)
        );
        return Boolean.TRUE.equals(success);
    }

    public void saveResponse(Long userId, Long orderId, String idempotencyKey, OrderResponseDto response) {
        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    key(userId, orderId, idempotencyKey),
                    value,
                    Duration.ofSeconds(TTL_SECONDS)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("멱등 응답 직렬화 실패", e);
        }
    }

    public void releaseLock(Long userId, Long orderId, String idempotencyKey) {
        redisTemplate.delete(key(userId, orderId, idempotencyKey) + ":lock");
    }
}
