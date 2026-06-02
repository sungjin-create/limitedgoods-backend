package com.limitedgoods.limitedgoods.orderitem.repository;

import com.limitedgoods.limitedgoods.orderitem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
