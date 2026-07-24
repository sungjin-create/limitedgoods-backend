package com.limitedgoods.limitedgoods.event.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.analytics.service.InternalAnalyticsEventHandler;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.exception.InternalEventProcessingException;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidEvent;
import com.limitedgoods.limitedgoods.notification.service.InternalEmailEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalOutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventService outboxEventService;
    private final InternalEmailEventHandler emailEventHandler;
    private final InternalAnalyticsEventHandler analyticsEventHandler;
    private final ObjectMapper objectMapper;

    public void process(Long outboxEventId) {
        OutboxEvent outboxEvent =
                outboxEventRepository
                .findById(outboxEventId)
                .orElseThrow();

        switch (outboxEvent.getEventType()) {
            case ORDER_PAID ->
                    processOrderPaid(outboxEvent);

            case ORDER_EXPIRED, ORDER_CANCELED ->
                    log.debug(
                            "event=internal_event_ignored "
                                    + "eventId={} eventType={}",
                            outboxEvent.getId(),
                            outboxEvent.getEventType()
                    );
        }

        outboxEventService.markPublished(
                outboxEvent.getId()
        );
    }

    private void processOrderPaid(
            OutboxEvent outboxEvent
    ) {
        OrderPaidEvent event = readOrderPaidEvent(outboxEvent.getPayload());

        InternalEventProcessingException failure =
                new InternalEventProcessingException(outboxEvent.getId());

        boolean failed = false;

        try {
            emailEventHandler.handle(outboxEvent.getId(), event);
        } catch (RuntimeException exception) {
            failed = true;
            failure.addSuppressed(exception);

            log.error(
                    "event=internal_email_event_failed "
                            + "eventId={}",
                    outboxEvent.getId(),
                    exception
            );
        }

        try {
            analyticsEventHandler.handle(outboxEvent.getId(), event);
        } catch (RuntimeException exception) {
            failed = true;
            failure.addSuppressed(exception);

            log.error(
                    "event=internal_analytics_event_failed "
                            + "eventId={}",
                    outboxEvent.getId(),
                    exception
            );
        }

        if (failed) {
            throw failure;
        }
    }

    private OrderPaidEvent readOrderPaidEvent(
            String payload
    ) {
        try {
            return objectMapper.readValue(payload, OrderPaidEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "ORDER_PAID event payload is invalid",
                    exception
            );
        }
    }
}