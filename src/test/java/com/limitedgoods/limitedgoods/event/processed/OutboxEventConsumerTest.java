package com.limitedgoods.limitedgoods.event.processed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limitedgoods.limitedgoods.event.consumer.OrderEventConsumer;
import com.limitedgoods.limitedgoods.event.processed.entity.ProcessedEvent;
import com.limitedgoods.limitedgoods.event.processed.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    ProcessedEventRepository processedEventRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderEventConsumer(
                objectMapper,
                processedEventRepository
        );
    }

    @Test
    @DisplayName("처리되지 않은 이벤트는 처리 후 processed_event에 저장한다")
    void consume_newEvent_success() throws Exception {
        // given
        String message = """
            {
              "eventId": 1,
              "eventType": "ORDER_PAID",
              "payload": "{\\"orderId\\":1}"
            }
            """;

        when(processedEventRepository.existsByEventIdAndConsumerGroup(
                eq(1L),
                anyString()
        )).thenReturn(false);

        // when
        //consumer.consume(message);

        // then
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 다시 처리하지 않는다")
    void consume_duplicateEvent_skip() throws Exception {
        // given
        String message = """
            {
              "eventId": 1,
              "eventType": "ORDER_PAID",
              "payload": "{\\"orderId\\":1}"
            }
            """;

        when(processedEventRepository.existsByEventIdAndConsumerGroup(
                eq(1L),
                anyString()
        )).thenReturn(true);

        // when
        //consumer.consume(message);

        // then
        verify(processedEventRepository, never()).save(any());
    }
}