package com.limitedgoods.limitedgoods.orderitem.repository;

import com.limitedgoods.limitedgoods.orderitem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByOrderId(Long orderId);
}
