package com.limitedgoods.limitedgoods.order.listener;

import com.limitedgoods.limitedgoods.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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

        Long orderId = Long.parseLong(expiredKey.substring("reservation:order:".length()));

        orderService.expireOrder(orderId);
    }

}