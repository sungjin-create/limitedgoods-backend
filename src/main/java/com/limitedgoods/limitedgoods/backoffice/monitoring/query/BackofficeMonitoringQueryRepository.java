package com.limitedgoods.limitedgoods.backoffice.monitoring.query;

import com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringOrderFlatResponse;
import com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringSummaryResponse;
import com.limitedgoods.limitedgoods.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BackofficeMonitoringQueryRepository extends JpaRepository<Order, Long> {
    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringOrderFlatResponse(
        o.id,
        u.email,
        o.totalPrice,
        o.status,
        o.createdAt,
        p.id,
        p.name,
        oi.quantity,
        oi.price
    )
    from OrderItem oi
    join oi.order o
    join o.user u
    join oi.product p
    order by o.createdAt desc
    """)
    List<BackofficeMonitoringOrderFlatResponse> findBackofficeMonitoringOrderFlat();

    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringOrderFlatResponse(
        o.id,
        u.email,
        o.totalPrice,
        o.status,
        o.createdAt,
        p.id,
        p.name,
        oi.quantity,
        oi.price
    )
    from OrderItem oi
    join oi.order o
    join o.user u
    join oi.product p
    where (o.createdAt >= :startAt)
      and (o.expiresAt <= :endAt)
    order by o.createdAt desc
    """)
    List<BackofficeMonitoringOrderFlatResponse> findBackofficeMonitoringOrderFlat(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringSummaryResponse(
        coalesce(sum(case when o.status = 'PAYMENT_PENDING' then 1 else 0 end), 0),
        coalesce(sum(case when o.status = 'PAID' then 1 else 0 end), 0),
        coalesce(sum(case when o.status = 'CANCELED' then 1 else 0 end), 0),
        coalesce(sum(case when o.status = 'PAYMENT_FAILED' then 1 else 0 end), 0)
    )
    from Order o
    where (o.createdAt >= :startAt)
      and (o.createdAt <= :endAt)
    """)
    BackofficeMonitoringSummaryResponse getBackofficeMonitoringSummary(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringSummaryResponse(
        coalesce(sum(case when o.status = 'PAYMENT_PENDING' then 1 else 0 end), 0),
        coalesce(sum(case when o.status = 'PAID' then 1 else 0 end), 0),
        coalesce(sum(case when o.status = 'CANCELED' then 1 else 0 end), 0),
        coalesce(sum(case when o.status = 'PAYMENT_FAILED' then 1 else 0 end), 0)
    )
    from Order o
    where (o.createdAt >= :startAt)
      and (o.createdAt <= :endAt)
    """)
    BackofficeMonitoringSummaryResponse getBackofficeMonitoringSummary();
}
