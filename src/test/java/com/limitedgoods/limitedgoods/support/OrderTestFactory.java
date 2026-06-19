package com.limitedgoods.limitedgoods.support;

import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderItem;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class OrderTestFactory {

    public static Order paymentApprovedOrder(Long orderId, Long userId) {
        Order order = createBaseOrder(orderId, userId);
        ReflectionTestUtils.setField(order, "status", OrderStatus.PAYMENT_APPROVED);
        return order;
    }

    public static Order paidOrder(Long orderId, Long userId) {
        Order order = createBaseOrder(orderId, userId);
        ReflectionTestUtils.setField(order, "status", OrderStatus.PAID);
        return order;
    }

    public static Order cancelRequestedOrder(Long orderId, Long userId) {
        Order order = createBaseOrder(orderId, userId);
        ReflectionTestUtils.setField(order, "status", OrderStatus.CANCEL_REQUESTED);
        return order;
    }

    public static Order cancelFailedOrder(Long orderId, Long userId) {
        Order order = createBaseOrder(orderId, userId);
        ReflectionTestUtils.setField(order, "status", OrderStatus.CANCEL_FAILED);
        return order;
    }

    private static Order createBaseOrder(Long orderId, Long userId) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "email", "test@test.com");

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "user", user);
        ReflectionTestUtils.setField(order, "totalPrice", 10000);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(order, "expiresAt", LocalDateTime.now().plusMinutes(10));

        return order;
    }

    public static OrderItem orderItem(Order order, Long productId, int quantity) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);
        ReflectionTestUtils.setField(product, "name", "테스트 상품");
        ReflectionTestUtils.setField(product, "price", 10000);

        OrderItem orderItem = new OrderItem();
        ReflectionTestUtils.setField(orderItem, "order", order);
        ReflectionTestUtils.setField(orderItem, "product", product);
        ReflectionTestUtils.setField(orderItem, "quantity", quantity);
        ReflectionTestUtils.setField(orderItem, "price", 10000);

        return orderItem;
    }
}