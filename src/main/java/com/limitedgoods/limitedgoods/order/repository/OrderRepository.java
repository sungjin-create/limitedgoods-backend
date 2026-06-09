package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatusInAndExpiresAtBefore(
            List<OrderStatus> statuses,
            LocalDateTime now
    );
}
