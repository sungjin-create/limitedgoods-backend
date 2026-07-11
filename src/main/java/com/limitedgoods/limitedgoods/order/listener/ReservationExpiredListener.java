package com.limitedgoods.limitedgoods.order.listener;

import com.limitedgoods.limitedgoods.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiredListener implements MessageListener {

    private final OrderService orderService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (!expiredKey.startsWith("reservation:order:")) {
            return;
        }

        Long orderId;

        try {
            orderId = Long.parseLong(expiredKey.substring("reservation:order:".length()));
        } catch(NumberFormatException e) {
            log.error(
                    "event=redis_expiration_invalid_key component=redis-listener " +
                            "expiredKey={}",
                    expiredKey,
                    e
            );
            return;
        }
        try {
            orderService.expireOrder(orderId);
        } catch (Exception e) {
            log.error(
                    "event=order_expiration_failed component=redis-listener " +
                            "orderId={}",
                    orderId,
                    e
            );
        }
    }
}