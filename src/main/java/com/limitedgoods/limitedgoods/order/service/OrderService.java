package com.limitedgoods.limitedgoods.order.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.orderitem.entity.OrderItem;
import com.limitedgoods.limitedgoods.orderitem.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

//    @Retryable(
//            value = ObjectOptimisticLockingFailureException.class,
//            maxAttempts = 10,
//            backoff = @Backoff(
//                    delay = 50,
//                    multiplier = 2,
//                    maxDelay = 200
//            )
//    )

    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto orderRequestDto){
        Long productId = orderRequestDto.getProductId();
        int quantity = orderRequestDto.getQuantity();
        int totalPrice = 0;

        //사용자 확인
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(ErrorCode.USER_NOT_FOUND)
        );

        //상품 아이디로 상품 확인
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PRODUCT_ID));
//        Product product = productRepository.findById(productId).orElseThrow(()->
//                new BusinessException(ErrorCode.INVALID_PRODUCT_ID));

        //상품 재고 수정
        product.decreaseStock(quantity);

        //총액 확인
        totalPrice = quantity * product.getPrice();

        //주문 생성
        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
        Order saveOrder = orderRepository.save(order);

        //상품_아이템 생성
        OrderItem orderItem = OrderItem.builder()
                .order(saveOrder)
                .product(product)
                .quantity(quantity)
                .price(product.getPrice())
                .build();

        orderItemRepository.save(orderItem);

        //주문 완료
        return OrderResponseDto.builder()
                .id(saveOrder.getId())
                .userId(user.getId())
                .status(saveOrder.getStatus().name())
                .totalPrice(saveOrder.getTotalPrice())
                .createdAt(saveOrder.getCreatedAt())
                .build();
    }

//    @Recover
//    public OrderResponseDto recover(ObjectOptimisticLockingFailureException e,
//                                    Long userId,
//                                    OrderRequestDto dto) {
//        throw new RuntimeException("재시도 실패", e);
//    }

}
