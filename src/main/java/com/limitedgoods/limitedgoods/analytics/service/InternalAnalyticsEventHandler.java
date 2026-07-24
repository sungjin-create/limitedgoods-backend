package com.limitedgoods.limitedgoods.analytics.service;

import com.limitedgoods.limitedgoods.analytics.entity.ProductSalesProjection;
import com.limitedgoods.limitedgoods.analytics.repository.ProductSalesProjectionRepository;
import com.limitedgoods.limitedgoods.event.payload.order.OrderPaidEvent;
import com.limitedgoods.limitedgoods.event.processed.entity.ProcessedEvent;
import com.limitedgoods.limitedgoods.event.processed.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalAnalyticsEventHandler {

    private static final String CONSUMER_NAME = "analytics";

    private final ProductSalesProjectionRepository
            productSalesProjectionRepository;

    private final ProcessedEventRepository
            processedEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(
            Long eventId,
            OrderPaidEvent event
    ) {
        if (processedEventRepository
                .existsByEventIdAndConsumerName(
                        eventId,
                        CONSUMER_NAME
                )) {
            return;
        }

        event.items().forEach(item -> {
            ProductSalesProjection projection =
                    productSalesProjectionRepository
                            .findByProductId(item.productId())
                            .orElseGet(() ->
                                    new ProductSalesProjection(
                                            item.productId()
                                    )
                            );

            projection.applySale(
                    item.quantity(),
                    item.unitPrice()
            );

            productSalesProjectionRepository.save(
                    projection
            );
        });

        processedEventRepository.save(
                new ProcessedEvent(
                        eventId,
                        CONSUMER_NAME
                )
        );
    }
}