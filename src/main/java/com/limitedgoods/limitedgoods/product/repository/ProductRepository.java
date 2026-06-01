package com.limitedgoods.limitedgoods.product.repository;

import com.limitedgoods.limitedgoods.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
