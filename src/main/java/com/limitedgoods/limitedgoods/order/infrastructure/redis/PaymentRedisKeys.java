package com.limitedgoods.limitedgoods.order.infrastructure.redis;

import com.limitedgoods.limitedgoods.common.infrastructure.redis.RedisKeyNamespace;

public final class PaymentRedisKeys {

    private static final String PREFIX =
            RedisKeyNamespace.ROOT + ":payment:idempotency";

    private PaymentRedisKeys() {
    }

    public static String response(
            Long userId,
            Long orderId,
            String idempotencyKey
    ) {
        return PREFIX
                + ":" + userId
                + ":" + orderId
                + ":" + idempotencyKey;
    }

    public static String lock(
            Long userId,
            Long orderId,
            String idempotencyKey
    ) {
        return response(userId, orderId, idempotencyKey)
                + ":lock";
    }
}