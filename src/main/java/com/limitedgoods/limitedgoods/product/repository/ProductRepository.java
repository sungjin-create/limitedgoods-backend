package com.limitedgoods.limitedgoods.product.repository;

import com.limitedgoods.limitedgoods.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from Product p
        where p.id = :id
        """)
    Optional<Product> findByIdWithLock(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Product p
        set p.stock = p.stock - :quantity
        where p.id = :id
          and p.stock >= :quantity
        """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

}
