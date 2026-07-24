package com.limitedgoods.limitedgoods.notification.infrastructure.mail;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class EmailProviderCircuit {

    private final AtomicReference<Instant> blockedUntil = new AtomicReference<>(Instant.EPOCH);

    public boolean isBlocked(Instant now) {
        return now.isBefore(blockedUntil.get());
    }

    public void open(Instant now, Duration duration) {
        Instant requestedUntil =
                now.plus(duration);

        blockedUntil.updateAndGet(current ->
                current.isAfter(requestedUntil)
                        ? current
                        : requestedUntil
        );
    }

    public void close() {
        blockedUntil.set(Instant.EPOCH);
    }

    public Instant getBlockedUntil() {
        return blockedUntil.get();
    }
}