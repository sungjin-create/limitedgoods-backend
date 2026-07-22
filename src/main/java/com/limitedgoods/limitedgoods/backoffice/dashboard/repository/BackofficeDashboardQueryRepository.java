package com.limitedgoods.limitedgoods.backoffice.dashboard.repository;

import com.limitedgoods.limitedgoods.backoffice.dashboard.dto.BackofficeRecentOrderResponse;
import com.limitedgoods.limitedgoods.order.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BackofficeDashboardQueryRepository extends JpaRepository<Order, Long> {
    @Query("""
        select new com.limitedgoods.limitedgoods.backoffice.dashboard.dto.BackofficeRecentOrderResponse(
            o.id,
            u.email,
            p.name,
            o.totalPrice,
            o.status,
            o.createdAt
        )
        from OrderItem oi
        join oi.order o
        join o.user u
        join oi.product p
        order by o.createdAt desc
        """)
    List<BackofficeRecentOrderResponse> findRecentOrders(
            Pageable pageable
    );
}
