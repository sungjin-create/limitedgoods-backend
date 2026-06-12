package com.limitedgoods.limitedgoods.stock.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final RedisTemplate<String, String> redisTemplate;

    // 재고가 충분하면 차감하고 남은 재고 반환
    // -1이면 재고 없음 또는 부족
    private static final String DECREASE_SCRIPT =
            "local stock = tonumber(redis.call('GET', KEYS[1])) " +
                    "if stock == nil then return -1 end " +
                    "if stock < tonumber(ARGV[1]) then return -1 end " +
                    "return redis.call('DECRBY', KEYS[1], ARGV[1])";

    private static final RedisScript<Long> DECREASE_REDIS_SCRIPT =
            RedisScript.of(DECREASE_SCRIPT, Long.class);

    private static final String INCREASE_SCRIPT =
            "return redis.call('INCRBY', KEYS[1], ARGV[1])";

    private static final RedisScript<Long> INCREASE_REDIS_SCRIPT =
            RedisScript.of(INCREASE_SCRIPT, Long.class);

    private String stockKey(Long productId) {
        return "stock:" + productId;
    }

    // 상품 등록 시 재고 초기화
    public void initStock(Long productId, int stock) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(stock));
    }

    // 재고 차감 (원자적)
    public void decreaseStock(Long productId, int quantity) {
        Long result = redisTemplate.execute(
                DECREASE_REDIS_SCRIPT,
                List.of(stockKey(productId)),
                String.valueOf(quantity)
        );

        if (result == null || result < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    // 보상용 재고 복구
    public void increaseStock(Long productId, int quantity) {
        redisTemplate.execute(
                INCREASE_REDIS_SCRIPT,
                List.of(stockKey(productId)),
                String.valueOf(quantity)
        );
    }
}