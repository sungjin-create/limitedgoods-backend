package com.limitedgoods.limitedgoods.order.concurrency;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.order.dto.OrderItemsListDto;
import com.limitedgoods.limitedgoods.order.dto.OrderResponseDto;
import com.limitedgoods.limitedgoods.order.entity.OrderStatus;
import com.limitedgoods.limitedgoods.order.repository.OrderItemRepository;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.order.service.OrderService;
import com.limitedgoods.limitedgoods.order.service.SoldOutCacheService;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.entity.UserRole;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "outbox.publish.delay=9999999"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderCreateConcurrencyTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    SoldOutCacheService soldOutCacheService;

    private final List<Long> createdOrderIds = new CopyOnWriteArrayList<>();
    private final List<Long> createdProductIds = new CopyOnWriteArrayList<>();
    private final List<Long> createdUserIds = new CopyOnWriteArrayList<>();

    @AfterEach
    void cleanup() {
//        for (Long orderId : createdOrderIds) {
//                orderItemRepository.deleteAllInBatch(
//                        orderItemRepository.findByOrderId(orderId)
//                );
//        }
//
//        orderRepository.deleteAllByIdInBatch(createdOrderIds);
//        productRepository.deleteAllByIdInBatch(createdProductIds);
//        userRepository.deleteAllByIdInBatch(createdUserIds);
    }

    @Test
    @DisplayName("concurrent createOrder never sells more than DB stock")
    void createOrder_concurrently_succeedsOnlyAsMuchAsStock() throws Exception {
        User user = saveUser("concurrency-stock@test.com");
        Product product = saveProduct(10, 10000);

        int threadCount = 30;
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        ConcurrentLinkedQueue<OrderResponseDto> responses = new ConcurrentLinkedQueue<>();

        runConcurrently(threadCount, index -> {
            try {
                OrderResponseDto response = orderService.createOrder(
                        user.getId(),
                        List.of(orderItem(product.getId(), 1)),
                        300,
                        "checkout-" + index + "-" + UUID.randomUUID()
                );

                responses.add(response);
                createdOrderIds.add(response.getId());
                successCount.incrementAndGet();
            } catch (BusinessException e) {
                failCount.incrementAndGet();
            }
        });

        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(20);
        assertThat(reloadedProduct.getStock()).isZero();
        assertThat(createdOrderIds).hasSize(10);
        assertThat(countCreatedOrderItems()).isEqualTo(10);
        assertThat(responses)
                .extracting(OrderResponseDto::getStatus)
                .containsOnly(OrderStatus.CREATED.name());

        verify(soldOutCacheService, atLeastOnce()).markSoldOutAfterCommit(product.getId());
    }

    @Test
    @DisplayName("concurrent expireOrder restores stock only once")
    void expireOrder_concurrently_restoresStockOnlyOnce() throws Exception {
        User user = saveUser("concurrency-expire@test.com");
        Product product = saveProduct(2, 10000);

        OrderResponseDto created = orderService.createOrder(
                user.getId(),
                List.of(orderItem(product.getId(), 2)),
                300,
                "checkout-expire-" + UUID.randomUUID()
        );
        createdOrderIds.add(created.getId());

        Product afterCreate = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterCreate.getStock()).isZero();

        runConcurrently(10, index -> orderService.expireOrder(created.getId()));

        Product afterExpire = productRepository.findById(product.getId()).orElseThrow();

        assertThat(afterExpire.getStock()).isEqualTo(2);
        assertThat(orderRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXPIRED);
        verify(soldOutCacheService, atLeastOnce()).clearSoldOutAfterCommit(product.getId());
    }

    private int countCreatedOrderItems() {
        return createdOrderIds.stream()
                .mapToInt(orderId -> orderItemRepository.findByOrderId(orderId).size())
                .sum();
    }

    private void runConcurrently(int threadCount, ThrowingIndexedTask task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.run(index);
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(errors).isEmpty();
    }

    private User saveUser(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password("encoded-password")
                .name("tester")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build());

        createdUserIds.add(user.getId());
        return user;
    }

    private Product saveProduct(int stock, int price) {
        Product product = new Product();
        product.setName("limited-item-" + UUID.randomUUID());
        product.setDescription("test product");
        product.setPrice(price);
        product.setStock(stock);

        Product saved = productRepository.save(product);
        createdProductIds.add(saved.getId());
        return saved;
    }

    private OrderItemsListDto orderItem(Long productId, int quantity) {
        return OrderItemsListDto.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    @FunctionalInterface
    private interface ThrowingIndexedTask {
        void run(int index) throws Exception;
    }
}
