package com.limitedgoods.limitedgoods.order.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final RedissonClient redissonClient;

    public <T> T executeWithLock(String key, Supplier<T> task) {
        RLock lock = redissonClient.getLock(key);

        try {
            boolean available = lock.tryLock(5,10, TimeUnit.SECONDS);

            if (!available) {

                throw new RuntimeException("락 획득 실패");
            }

            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}