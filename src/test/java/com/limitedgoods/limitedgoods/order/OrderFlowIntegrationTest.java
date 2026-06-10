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
import com.limitedgoods.limitedgoods.orderitem.repository.OrderItemRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderFlowIntegrationTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RedisStockService redisStockService;
    @Autowired private RedisReservationService redisReservationService;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void cleanup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ────────────── 정상 흐름 ──────────────

    @Test
    void 주문생성_후_결제성공하면_PAID가_되고_예약키는_삭제된다() {
        User user = saveUser("flow1@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));

        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREATED.name());
        assertThat(getRedisStock(product.getId())).isEqualTo(99);
        assertThat(redisReservationService.getReservation(created.getId())).isNotNull();

        OrderResponseDto paid = orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(false), uuid());

        Order order = orderRepository.findById(paid.getId()).orElseThrow();
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloaded.getStock()).isEqualTo(99);            // DB 재고 차감
        assertThat(getRedisStock(product.getId())).isEqualTo(99); // Redis 재고 유지
        assertThat(redisReservationService.getReservation(created.getId())).isNull();
    }

    @Test
    void 결제실패_후_재결제성공하면_PAYMENT_FAILED를_거쳐_PAID로_전이된다() {
        User user = saveUser("flow2@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));

        // 첫 결제 실패
        assertThatThrownBy(() ->
                orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(true), uuid()))
                .isInstanceOf(BusinessException.class);

        Order failedOrder = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(failedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(redisReservationService.getReservation(created.getId())).isNotNull(); // 예약은 살아있어야 함
        assertThat(getRedisStock(product.getId())).isEqualTo(99);

        // 재결제 성공
        OrderResponseDto retried = orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(false), uuid());

        Order paidOrder = orderRepository.findById(retried.getId()).orElseThrow();
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(reloaded.getStock()).isEqualTo(99);
        assertThat(redisReservationService.getReservation(created.getId())).isNull();
    }

    // ────────────── 멱등성 ──────────────

    @Test
    void 동일한_IdempotencyKey로_두번_요청하면_한번만_처리된다() {
        User user = saveUser("idem@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));

        String idempotencyKey = uuid();

        OrderResponseDto first = orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(false), idempotencyKey);
        OrderResponseDto second = orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(false), idempotencyKey);

        // 응답은 동일해야 함
        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(first.getStatus()).isEqualTo(second.getStatus());

        // DB 재고는 1번만 차감
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(99);
    }

    // ────────────── 만료 처리 ──────────────

    @Test
    void 만료처리시_EXPIRED로_변경되고_Redis재고가_복구된다() {
        User user = saveUser("expire1@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));

        // 결제 실패 후 만료
        assertThatThrownBy(() ->
                orderFacade.payOrder(user.getId(), created.getId(), paymentRequest(true), uuid()))
                .isInstanceOf(BusinessException.class);

        orderService.expireOrder(created.getId());

        Order expiredOrder = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(getRedisStock(product.getId())).isEqualTo(100); // 재고 복구
    }

    @Test
    void expireOrder_중복호출해도_재고는_한번만_복구된다() {
        User user = saveUser("expire2@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));
        assertThat(getRedisStock(product.getId())).isEqualTo(99);

        // 두 번 만료 호출
        orderService.expireOrder(created.getId());
        orderService.expireOrder(created.getId()); // 두 번째는 무시돼야 함

        assertThat(getRedisStock(product.getId())).isEqualTo(100); // 1번만 복구
    }

    @Test
    void PAYMENT_PENDING_상태는_만료처리_대상이_아니다() {
        User user = saveUser("pending@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));

        // PAYMENT_PENDING으로 강제 전이
        orderService.startPayment(user.getId(), created.getId());

        // 만료 시도
        orderService.expireOrder(created.getId());

        Order order = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING); // 만료 안 됨
        assertThat(getRedisStock(product.getId())).isEqualTo(99); // 재고 복구 안 됨
    }

    @Test
    void 실제_TTL_만료이벤트로_EXPIRED가_반영된다() throws Exception {
        User user = saveUser("ttl@test.com");
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        OrderResponseDto created = orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));

        // TTL을 1초로 줄여 재설정
        redisReservationService.createReservation(created.getId(), product.getId(), 1, 1);

        waitUntilOrderStatus(created.getId(), OrderStatus.EXPIRED, Duration.ofSeconds(5));

        Order expiredOrder = orderRepository.findById(created.getId()).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(getRedisStock(product.getId())).isEqualTo(100);
    }

    // ────────────── helper ──────────────

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .name("tester")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Product saveProduct(int stock, int price) {
        Product product = new Product();
        product.setName("limited-shoes");
        product.setDescription("test product");
        product.setPrice(price);
        product.setStock(stock);
        return productRepository.save(product);
    }

    private OrderRequestDto orderRequest(Long productId, int quantity) {
        return OrderRequestDto.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private PaymentRequestDto paymentRequest(boolean forceFail) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setForceFail(forceFail);
        return dto;
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }

    private int getRedisStock(Long productId) {
        String value = redisTemplate.opsForValue().get("stock:" + productId);
        return Integer.parseInt(value);
    }

    private void waitUntilOrderStatus(Long orderId, OrderStatus expected, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            if (order.getStatus() == expected) return;
            Thread.sleep(200);
        }
        Order order = orderRepository.findById(orderId).orElseThrow();
        throw new AssertionError("expected=" + expected + ", actual=" + order.getStatus());
    }
}