package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

}
