package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}
