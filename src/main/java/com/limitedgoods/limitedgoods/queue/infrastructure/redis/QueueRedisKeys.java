package com.limitedgoods.limitedgoods.queue.infrastructure.redis;

import com.limitedgoods.limitedgoods.common.infrastructure.redis.RedisKeyNamespace;

public final class QueueRedisKeys {

    private static final String PREFIX =
            RedisKeyNamespace.ROOT + ":queue";

    private QueueRedisKeys() {
    }

    public static String waiting(Long productId) {
        return PREFIX + ":{" + productId + "}:waiting";
    }

    public static String admissionTrack(
            Long productId,
            Long userId
    ) {
        return PREFIX
                + ":{" + productId + "}"
                + ":admission:track:"
                + userId;
    }

    public static String admissionToken(
            Long productId,
            String token
    ) {
        return PREFIX
                + ":{" + productId + "}"
                + ":admission:token:"
                + token;
    }

    public static String admissionTrackPrefix() {
        return PREFIX + ":";
    }
}