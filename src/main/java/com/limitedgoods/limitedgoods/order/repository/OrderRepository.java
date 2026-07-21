package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.backoffice.dashboard.dto.BackofficeRecentOrderResponse;
import com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringOrderFlatResponse;
import com.limitedgoods.limitedgoods.backoffice.order.dto.BackofficeMonitoringSummaryResponse;
import com.limitedgoods.limitedgoods.order.dto.response.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatusInAndExpiresAtBefore(
            List<OrderStatus> statuses,
            LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        where o.id = :id
          and o.user.id = :userId
        """)
    Optional<Order> findByIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Order o
           set o.status = :expiredStatus,
               o.updatedAt = :now
         where o.id = :orderId
           and o.status in :activeStatuses
        """)
    int expireIfActive(
            @Param("orderId") Long orderId,
            @Param("expiredStatus") OrderStatus expiredStatus,
            @Param("activeStatuses") List<OrderStatus> activeStatuses,
            @Param("now") LocalDateTime now
    );

    @Query("""
    select new com.limitedgoods.limitedgoods.order.dto.response.OrderDetailResponseDto(
        o.id,
        o.totalPrice,
        o.status,
        o.createdAt,
        o.expiresAt,
        p.id,
        p.name,
        oi.quantity,
        oi.price
    )
    from OrderItem oi
    join oi.order o
    join oi.product p
    where o.user.id = :userId
    order by o.createdAt desc
    """)
    List<OrderDetailResponseDto> findMyOrderDetails(
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select o
    from Order o
    where o.user.id = :userId
        and o.status in :statuses
    order by o.id
    """)
    List<Order> findActiveOrdersForUpdate(Long userId, List<OrderStatus> orderStatusList);


    @Query("""
    select count(o)
    from Order o
    where o.createdAt >= :start
      and o.createdAt < :end
    """)
    long countOrdersCreatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    select count(o)
    from Order o
    where o.paidAt >= :start
      and o.paidAt < :end
      and o.status in :statuses
    """)
    long countPaidOrdersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<OrderStatus> statuses
    );

    @Query("""
    select coalesce(sum(o.totalPrice), 0)
    from Order o
    where o.paidAt >= :start
      and o.paidAt < :end
      and o.status in :statuses
    """)
    long sumRevenueBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<OrderStatus> statuses
    );


    @Query("""
    select count(o)
    from Order o
    where o.createdAt >= :start
      and o.createdAt < :end
    """)
    long countCreatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
    select count(o)
    from Order o
    where o.paidAt >= :start
      and o.paidAt < :end
      and o.status in :statuses
    """)
    long countPaidBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<OrderStatus> statuses
    );

    @Query("""
    select count(o)
    from Order o
    where o.createdAt >= :start
      and o.createdAt < :end
      and o.status in :statuses
    """)
    long countCreatedBetweenAndStatusIn(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<OrderStatus> statuses
    );

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
    List<BackofficeRecentOrderResponse> findRecentOrders(Pageable pageable);

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

    @Query("""
    select oi
    from OrderItem oi
    join oi.order o
    where o.id = :orderId
        and o.user.id = :userId
    """)
    List<OrderItem> findOrderItemsByOrder(Long orderId, Long userId);

    @Query("""
    select o
    from Order o
    where o.user.id = :userId
        and o.checkoutToken = :checkoutToken
    """)
    Optional<Order> findByUserIdAndCheckoutToken(
            @Param("userId") Long userId,
            @Param("checkoutToken") String checkoutToken
    );
}
