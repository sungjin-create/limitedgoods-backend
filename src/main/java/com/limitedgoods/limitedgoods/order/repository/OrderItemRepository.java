package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
    select oi
    from OrderItem oi
    join fetch oi.product p
    where oi.order.id = :orderId
    order by oi.id
    """)
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);
}
