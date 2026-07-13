package com.limitedgoods.limitedgoods.order.concurrency;

import com.limitedgoods.limitedgoods.idempotency.service.PaymentIdempotencyService;
import com.limitedgoods.limitedgoods.order.service.OrderRateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "outbox.publish.delay=9999999"
})
class RedisDuplicateProtectionTest {

    @Autowired
    OrderRateLimitService orderRateLimitService;

    @Autowired
    PaymentIdempotencyService paymentIdempotencyService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    private String orderRateLimitKey;
    private String paymentIdempotencyKey;

    @AfterEach
    void cleanup() {
        List<String> keys = new ArrayList<>();

        if (orderRateLimitKey != null) {
            keys.add(orderRateLimitKey);
        }

        if (paymentIdempotencyKey != null) {
            keys.add(paymentIdempotencyKey);
            keys.add(paymentIdempotencyKey + ":lock");
        }

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("Redis order rate limit allows only max requests under concurrency")
    void orderRateLimit_concurrently_allowsOnlyMaxRequests() throws Exception {
        Long userId = uniquePositiveLong();
        Long productId = uniquePositiveLong();
        orderRateLimitKey = "rate-limit:order:" + userId + ":" + productId;
        redisTemplate.delete(orderRateLimitKey);

        int threadCount = 20;
        AtomicInteger allowedCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        runConcurrently(threadCount, index -> {
            if (orderRateLimitService.allow(userId, productId)) {
                allowedCount.incrementAndGet();
            } else {
                rejectedCount.incrementAndGet();
            }
        });

        assertThat(allowedCount.get()).isEqualTo(3);
        assertThat(rejectedCount.get()).isEqualTo(17);
        assertThat(redisTemplate.getExpire(orderRateLimitKey)).isPositive();
    }

    @Test
    @DisplayName("Redis payment idempotency lock allows only one concurrent request")
    void paymentIdempotencyLock_concurrently_allowsOnlyOneRequest() throws Exception {
        Long userId = uniquePositiveLong();
        Long orderId = uniquePositiveLong();
        String idempotencyKey = "idem-" + UUID.randomUUID();
        paymentIdempotencyKey = paymentIdempotencyService.key(userId, orderId, idempotencyKey);
        redisTemplate.delete(List.of(paymentIdempotencyKey, paymentIdempotencyKey + ":lock"));

        int threadCount = 20;
        AtomicInteger lockedCount = new AtomicInteger();
        AtomicInteger duplicatedCount = new AtomicInteger();

        runConcurrently(threadCount, index -> {
            if (paymentIdempotencyService.acquireLock(userId, orderId, idempotencyKey)) {
                lockedCount.incrementAndGet();
            } else {
                duplicatedCount.incrementAndGet();
            }
        });

        assertThat(lockedCount.get()).isEqualTo(1);
        assertThat(duplicatedCount.get()).isEqualTo(19);
        assertThat(redisTemplate.getExpire(paymentIdempotencyKey + ":lock")).isPositive();
    }

    private void runConcurrently(int threadCount, ThrowingIndexedTask task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run(index);
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(errors).isEmpty();
    }

    private Long uniquePositiveLong() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    @FunctionalInterface
    private interface ThrowingIndexedTask {
        void run(int index) throws Exception;
    }
}
