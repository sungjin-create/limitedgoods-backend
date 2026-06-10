package com.limitedgoods.limitedgoods.order;

import com.limitedgoods.limitedgoods.order.dto.OrderRequestDto;
import com.limitedgoods.limitedgoods.order.repository.OrderRepository;
import com.limitedgoods.limitedgoods.order.service.OrderFacade;
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

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderServiceConcurrencyTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RedisStockService redisStockService;
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

    @Test
    void 재고_100개_상품에_100명_동시_주문하면_100개_모두_성공한다() throws InterruptedException {
        User user = saveUser();
        Product product = saveProduct(100, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        runConcurrently(100, () -> {
            try {
                orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));
                success.incrementAndGet();
            } catch (Exception e) {
                fail.incrementAndGet();
            }
        });

        assertThat(success.get()).isEqualTo(100);
        assertThat(fail.get()).isEqualTo(0);
        assertThat(getRedisStock(product.getId())).isEqualTo(0);
    }

    @Test
    void 재고_10개_상품에_100명_동시_주문하면_10개만_성공한다() throws InterruptedException {
        User user = saveUser();
        Product product = saveProduct(10, 10000);
        redisStockService.initStock(product.getId(), product.getStock());

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        runConcurrently(100, () -> {
            try {
                orderFacade.createOrder(user.getId(), orderRequest(product.getId(), 1));
                success.incrementAndGet();
            } catch (Exception e) {
                fail.incrementAndGet();
            }
        });

        assertThat(success.get()).isEqualTo(10);
        assertThat(fail.get()).isEqualTo(90);
        assertThat(getRedisStock(product.getId())).isEqualTo(0);
    }

    // ────────────── helper ──────────────

    private void runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try { task.run(); }
                finally { latch.countDown(); }
            });
        }
        latch.await();
        executor.shutdown();
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .email("concurrency@test.com")
                .password("encoded")
                .name("tester")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Product saveProduct(int stock, int price) {
        Product p = new Product();
        p.setName("limited-item");
        p.setDescription("test");
        p.setPrice(price);
        p.setStock(stock);
        return productRepository.save(p);
    }

    private OrderRequestDto orderRequest(Long productId, int quantity) {
        return OrderRequestDto.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private int getRedisStock(Long productId) {
        String value = redisTemplate.opsForValue().get("stock:" + productId);
        return Integer.parseInt(value);
    }
}