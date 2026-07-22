package com.limitedgoods.limitedgoods.order.application.history;

import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatusHistory;
import com.limitedgoods.limitedgoods.order.repository.OrderStatusHistoryRepository;
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
}
