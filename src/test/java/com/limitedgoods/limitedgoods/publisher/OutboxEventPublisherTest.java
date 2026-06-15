package com.limitedgoods.limitedgoods.publisher;

import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEvent;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventStatus;
import com.limitedgoods.limitedgoods.event.outbox.entity.OutboxEventType;
import com.limitedgoods.limitedgoods.event.outbox.publisher.OutboxEventPublisher;
import com.limitedgoods.limitedgoods.event.outbox.repository.OutboxEventRepository;
import com.limitedgoods.limitedgoods.event.outbox.service.OutboxEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxEventPublisherTest {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final OutboxEventService outboxEventService = mock(OutboxEventService.class);

    private final OutboxEventPublisher publisher = new OutboxEventPublisher(
            outboxEventRepository,
            kafkaTemplate,
            outboxEventService
    );

    @Test
    @DisplayName("Kafka 발행 성공 시 Outbox 상태를 PUBLISHED로 변경한다")
    void publish_success_markPublished() {
        // given
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.ORDER_PAID,
                "ORDER",
                1L,
                "{\"orderId\":1}"
        );

        when(outboxEventRepository.findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                anyList(),
                anyInt()
        )).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq("order-events"),
                eq("1"),
                eq("{\"orderId\":1}")
        )).thenReturn(future);

        // when
        publisher.publish();

        // then
        verify(kafkaTemplate).send(
                eq("order-events"),
                eq("1"),
                eq("{\"orderId\":1}")
        );

        verify(outboxEventService).markPublished(event.getId());
        verify(outboxEventService, never()).markFailed(anyLong(), any());
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 Outbox 상태를 FAILED로 변경한다")
    void publish_fail_markFailed() {
        // given
        OutboxEvent event = OutboxEvent.create(
                OutboxEventType.ORDER_PAID,
                "ORDER",
                1L,
                "{\"orderId\":1}"
        );

        RuntimeException kafkaException = new RuntimeException("Kafka send failed");

        when(outboxEventRepository.findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                anyList(),
                anyInt()
        )).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(kafkaException);

        when(kafkaTemplate.send(
                eq("order-events"),
                eq("1"),
                eq("{\"orderId\":1}")
        )).thenReturn(future);

        // when
        publisher.publish();

        // then
        verify(outboxEventService).markFailed(eq(event.getId()), any(Throwable.class));
        verify(outboxEventService, never()).markPublished(anyLong());
    }

    @Test
    @DisplayName("PENDING, FAILED 상태의 이벤트만 조회해서 재시도한다")
    void publish_findPendingAndFailedEvents() {
        // given
        when(outboxEventRepository.findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                anyList(),
                anyInt()
        )).thenReturn(List.of());

        // when
        publisher.publish();

        // then
        verify(outboxEventRepository).findTop100ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(
                eq(List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED)),
                eq(5)
        );
    }
}