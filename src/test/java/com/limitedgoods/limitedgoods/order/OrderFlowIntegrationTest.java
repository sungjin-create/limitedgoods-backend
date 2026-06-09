package com.limitedgoods.limitedgoods.order;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.dto.ReservationPayload;
import com.limitedgoods.limitedgoods.order.entity.Order;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.payment.dto.PaymentRequestDto;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.order.service.OrderFacade;
import com.limitedgoods.limitedgoods.order.service.OrderService;
import com.limitedgoods.limitedgoods.order.service.RedisReservationService;
import com.limitedgoods.limitedgoods.order.service.RedisStockService;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.entity.UserRole;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderFlowIntegrationTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RedisStockService redisStockService;
    @Autowired private RedisReservationService redisReservationService;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @Test
    void 주문생성_후_결제성공하면_PAID가_되고_예약키는_삭제된다() {
        // given
        User user = saveUser();
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderRequestDto request = OrderRequestDto.builder()
                .productId(product.getId())
                .quantity(1)
                .build();

        // when
        OrderResponseDto created = orderFacade.createOrder(user.getId(), request);

        // then
        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREATED.name());
        assertThat(getRedisStock(product.getId())).isEqualTo(99);
        assertThat(redisReservationService.getReservation(created.getId())).isNotNull();

        // when
        OrderResponseDto paid = orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(false));

        // then
        Order order = orderRepository.findById(paid.getId()).orElseThrow();
        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloadedProduct.getStock()).isEqualTo(99);
        assertThat(getRedisStock(product.getId())).isEqualTo(99);
        assertThat(redisReservationService.getReservation(created.getId())).isNull();
    }

    @Test
    void 결제실패_후_재결제성공하면_PAYMENT_FAILED를_거쳐_PAID로_전이된다() {
        // given
        User user = saveUser();
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(
                user.getId(),
                OrderRequestDto.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()
        );

        // when
        assertThatThrownBy(() -> orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(true)))
                .isInstanceOf(BusinessException.class);

        // then
        Order failedOrder = orderRepository.findById(created.getId()).orElseThrow();
        ReservationPayload reservation = redisReservationService.getReservation(created.getId());

        assertThat(failedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(reservation).isNotNull();
        assertThat(getRedisStock(product.getId())).isEqualTo(99);

        // when
        OrderResponseDto retried = orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(false));

        // then
        Order paidOrder = orderRepository.findById(retried.getId()).orElseThrow();
        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloadedProduct.getStock()).isEqualTo(99);
        assertThat(redisReservationService.getReservation(created.getId())).isNull();
    }

    @Test
    void 예약만료처리시_EXPIRED로_변경되고_Redis재고가_복구된다() {
        // given
        User user = saveUser();
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(
                user.getId(),
                OrderRequestDto.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()
        );

        // 선택: PAYMENT_FAILED 상태까지 보낸 뒤 만료 검증
        assertThatThrownBy(() -> orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(true)))
                .isInstanceOf(BusinessException.class);

        // when
        orderService.expireOrder(created.getId());

        // then
        Order expiredOrder = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(getRedisStock(product.getId())).isEqualTo(100);
    }

    @Test
    void 실제_TTL_만료이벤트로_EXPIRED가_반영된다() throws Exception {
        // given
        User user = saveUser();
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(
                user.getId(),
                OrderRequestDto.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()
        );

        // TTL을 짧게 다시 설정
        redisReservationService.createReservation(created.getId(), product.getId(), 1, 1);

        // when
        waitUntilOrderStatus(created.getId(), OrderStatus.EXPIRED, Duration.ofSeconds(5));

        // then
        Order expiredOrder = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(getRedisStock(product.getId())).isEqualTo(100);
    }

    private User saveUser() {
        User user = User.builder()
                .email("test@example.com")
                .password("encoded-password")
                .name("tester")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private Product saveProduct(int stock, int price) {
        Product product = new Product();
        product.setName("limited-shoes");
        product.setDescription("test product");
        product.setPrice(price);
        product.setStock(stock);
        return productRepository.save(product);
    }

    private PaymentRequestDto paymentRequest(boolean forceFail) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setForceFail(forceFail);
        return dto;
    }

    private int getRedisStock(Long productId) {
        String value = redisTemplate.opsForValue().get("stock:" + productId);
        return Integer.parseInt(value);
    }

    private void waitUntilOrderStatus(Long orderId, OrderStatus expectedStatus, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            if (order.getStatus() == expectedStatus) {
                return;
            }
            Thread.sleep(100);
        }

        Order order = orderRepository.findById(orderId).orElseThrow();
        throw new AssertionError("expected status=" + expectedStatus + ", actual=" + order.getStatus());
    }
}
