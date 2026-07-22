package com.limitedgoods.limitedgoods.order.application.history;

import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.entity.OrderStatusHistory;
import com.limitedgoods.limitedgoods.order.repository.OrderStatusHistoryRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository historyRepository;

    @Transactional
    public void createInitialHistory(Order order) {
        historyRepository.save(
                OrderStatusHistory.create(
                        order,
                        order.getUser(),
                        null,
                        order.getStatus(),
                        "주문 생성"
                )
        );
    }

    @Transactional
    public void record(
            Order order,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String reason,
            User changedByUser
    ) {
        historyRepository.save(
                OrderStatusHistory.create(
                        order,
                        changedByUser,
                        fromStatus,
                        toStatus,
                        reason
                )
        );
    }
}
