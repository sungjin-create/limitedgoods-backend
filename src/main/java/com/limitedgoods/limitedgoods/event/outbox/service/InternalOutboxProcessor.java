package com.limitedgoods.limitedgoods.event.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import com.limitedgoods.limitedgoods.event.processed.entity.ProcessedEvent;
import com.limitedgoods.limitedgoods.analytics.entity.ProductSalesProjection;
import com.limitedgoods.limitedgoods.notification.repository.EmailDeliveryRepository;
import com.limitedgoods.limitedgoods.event.processed.repository.ProcessedEventRepository;
import com.limitedgoods.limitedgoods.analytics.repository.ProductSalesProjectionRepository;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidEvent;
import com.limitedgoods.limitedgoods.notification.template.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalOutboxProcessor {
    private static final String EMAIL_CONSUMER = "email";
    private static final String ANALYTICS_CONSUMER = "analytics";
    public static final int PAYMENT_COMPLETED_VERSION = 1;

    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final EmailDeliveryRepository emailDeliveryRepository;
    private final ProductSalesProjectionRepository productSalesProjectionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void process(Long outboxEventId) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(outboxEventId).orElseThrow();

        if (outboxEvent.getEventType() == OutboxEventType.ORDER_PAID) {
            OrderPaidEvent event = readOrderPaidEvent(outboxEvent.getPayload());
            processEmail(outboxEvent.getId(), event);
            processAnalytics(outboxEvent.getId(), event);
        }

        outboxEvent.markPublished();
    }

    private OrderPaidEvent readOrderPaidEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderPaidEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("ORDER_PAID event payload is invalid", exception);
        }
    }

    private void processEmail(Long eventId, OrderPaidEvent event) {
        if (processedEventRepository.existsByEventIdAndConsumerName(eventId, EMAIL_CONSUMER)) return;
        emailDeliveryRepository.save(new EmailDelivery(
                eventId,
                event.orderId(),
                event.recipientEmail(),
                EmailTemplateType.PAYMENT_COMPLETED,
                PAYMENT_COMPLETED_VERSION
        ));
        processedEventRepository.save(new ProcessedEvent(eventId, EMAIL_CONSUMER));
    }

    private void processAnalytics(Long eventId, OrderPaidEvent event) {
        if (processedEventRepository.existsByEventIdAndConsumerName(eventId, ANALYTICS_CONSUMER)) return;
        event.items().forEach(item -> {
            ProductSalesProjection projection = productSalesProjectionRepository.findByProductId(item.productId())
                    .orElseGet(() -> new ProductSalesProjection(item.productId()));
            projection.applySale(item.quantity(), item.unitPrice());
            productSalesProjectionRepository.save(projection);
        });
        processedEventRepository.save(new ProcessedEvent(eventId, ANALYTICS_CONSUMER));
    }
}
