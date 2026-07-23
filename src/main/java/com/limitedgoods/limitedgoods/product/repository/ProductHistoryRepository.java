package com.limitedgoods.limitedgoods.product.repository;

import com.limitedgoods.limitedgoods.product.entity.ProductHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductHistoryRepository extends JpaRepository<ProductHistory, Long> {
}
