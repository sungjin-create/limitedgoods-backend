package com.limitedgoods.limitedgoods.common.config;

import com.limitedgoods.limitedgoods.order.service.RedisStockService;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStockInitializer implements ApplicationRunner {
    private final ProductRepository productRepository;
    private final RedisStockService redisStockService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Redis 재고 초기화 시작");

        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            redisStockService.initStock(product.getId(), product.getStock());
        }

        log.info("Redis 재고 초기화 완료 - 총 {}개 상품", products.size());
    }

}
