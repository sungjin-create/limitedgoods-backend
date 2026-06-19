package com.limitedgoods.limitedgoods.admin;

import com.limitedgoods.limitedgoods.order.dto.AdminOrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.AdminOrderSearchCondition;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.order.service.AdminOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    AdminOrderService adminOrderService;

    @Test
    @DisplayName("관리자는 상태, 사용자, 기간 조건으로 주문을 검색할 수 있다")
    void searchOrders_success() {
        // given
        AdminOrderSearchCondition condition = new AdminOrderSearchCondition();
        condition.setStatus(OrderStatus.PAID);
        condition.setUserId(1L);
        condition.setFrom(LocalDate.of(2026, 6, 1));
        condition.setTo(LocalDate.of(2026, 6, 15));

        Pageable pageable = PageRequest.of(0, 20);

        when(orderRepository.searchAdminOrders(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(Page.empty());

        // when
        Page<AdminOrderResponseDto> result =
                adminOrderService.searchOrders(condition, pageable);

        // then
        assertThat(result).isEmpty();

        verify(orderRepository).searchAdminOrders(
                eq(OrderStatus.PAID),
                eq(1L),
                eq(LocalDate.of(2026, 6, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 6, 16).atStartOfDay()),
                eq(pageable)
        );
    }
}
