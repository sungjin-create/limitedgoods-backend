package com.limitedgoods.limitedgoods.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.ReservationPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisReservationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void createReservation(Long orderId, Long productId, int quantity, long ttlSeconds) {
        ReservationPayload payload = new ReservationPayload(orderId, productId, quantity);

        try {
            String key = "reservation:order:" + orderId;
            String value = objectMapper.writeValueAsString(payload);

            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("예약 정보 직렬화 실패", e);
        }
    }

    public ReservationPayload getReservation(Long orderId) {
        String key = "reservation:order:" + orderId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            return objectMapper.readValue(value, ReservationPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("예약 정보 역직렬화 실패", e);
        }
    }

    public void extendReservation(Long orderId, long ttlSeconds) {
        String key = "reservation:order:" + orderId;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }

        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public void deleteReservation(Long orderId) {
        redisTemplate.delete("reservation:order:" + orderId);
    }
}
