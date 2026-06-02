package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
