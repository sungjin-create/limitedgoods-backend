package com.limitedgoods.limitedgoods.order.repository;

import com.limitedgoods.limitedgoods.order.dto.AdminOrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.OrderDetailResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
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

    List<Order> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Order> findByStatusInAndExpiresAtBefore(
            List<OrderStatus> statuses,
            LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        where o.id = :id
        """)
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

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
    select new com.limitedgoods.limitedgoods.order.dto.AdminOrderResponseDto(
        o.id,
        u.id,
        u.email,
        p.id,
        p.name,
        oi.quantity,
        oi.price,
        o.totalPrice,
        o.status,
        o.createdAt,
        o.paidAt,
        o.expiresAt
    )
    from OrderItem oi
    join oi.order o
    join o.user u
    join oi.product p
    where (:status is null or o.status = :status)
      and (:userId is null or u.id = :userId)
      and (:fromDate is null or o.createdAt >= :fromDate)
      and (:toDate is null or o.createdAt < :toDate)
    order by o.createdAt desc
    """)
    Page<AdminOrderResponseDto> searchAdminOrders(
            @Param("status") OrderStatus status,
            @Param("userId") Long userId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
    select new com.limitedgoods.limitedgoods.order.dto.AdminOrderResponseDto(
        o.id,
        u.id,
        u.email,
        p.id,
        p.name,
        oi.quantity,
        oi.price,
        o.totalPrice,
        o.status,
        o.createdAt,
        o.paidAt,
        o.expiresAt
    )
    from OrderItem oi
    join oi.order o
    join o.user u
    join oi.product p
    where o.id = :orderId
    """)
    Optional<AdminOrderResponseDto> findAdminOrderDetail(
            @Param("orderId") Long orderId
    );

    @Query("""
    select new com.limitedgoods.limitedgoods.order.dto.OrderDetailResponseDto(
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
}
