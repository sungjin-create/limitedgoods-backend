package com.limitedgoods.limitedgoods.stock.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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
        try {
            Long result = redisTemplate.execute(
                    DECREASE_REDIS_SCRIPT,
                    List.of(stockKey(productId)),
                    String.valueOf(quantity)
            );

            if (result == null) {
                log.error(
                        "event=redis_stock_decrease_null component=redis " +
                                "productId={} quantity={}",
                        productId,
                        quantity
                );

                throw new IllegalStateException("Redis 재고 차감 결과가 null입니다.");
            }

            if (result < 0) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }

        }catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "event=redis_stock_decrease_failed component=redis " +
                            "productId={} quantity={}",
                    productId,
                    quantity,
                    e
            );

            throw e;
        }
    }

    // 보상용 재고 복구
    public void increaseStock(Long productId, int quantity) {
        try {
            Long result = redisTemplate.execute(
                    INCREASE_REDIS_SCRIPT,
                    List.of(stockKey(productId)),
                    String.valueOf(quantity)
            );

            if (result == null) {
                throw new IllegalStateException("Redis 재고 복구 결과가 null입니다.");
            }

        } catch (Exception e) {
            log.error(
                    "event=redis_stock_compensation_failed component=redis " +
                            "productId={} quantity={}",
                    productId,
                    quantity,
                    e
            );

            throw e;
        }
    }
}