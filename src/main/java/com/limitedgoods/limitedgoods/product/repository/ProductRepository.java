package com.limitedgoods.limitedgoods.product.repository;

import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductResponse;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from Product p
        where p.id = :id
        """)
    Optional<Product> findByIdWithLock(Long id);

    @Modifying(flushAutomatically = true)
    @Query("""
        update Product p
        set p.stock = p.stock - :quantity
        where p.id = :id
          and p.stock >= :quantity
        """)
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
    update Product p
       set p.stock = p.stock + :quantity
     where p.id = :id
    """)
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("""
    UPDATE Product p
    SET p.soldCount = p.soldCount + :quantity
    WHERE p.id = :productId
    """)
    void increaseSoldCount(Long productId, int quantity);

    @Query("""
        select p
        from Product p
        where upper(p.name) like upper(concat('%', :keyword, '%'))
            or upper(p.description) like upper(concat('%', :keyword, '%'))
    """)
    Page<Product> searchByKeyword(Pageable pageable, @Param("keyword") String keyword);

    @Query("""
    select case when count(p) > 0 then true else false end
    from Product p
    where p.id = :productId
      and p.stock <= 0
    """)
    boolean isSoldOut(@Param("productId") Long productId);

    @Query("""
    select p.stock
    from Product p
    where p.id = :productId
    """)
    Optional<Integer> findStockById(@Param("productId") Long productId
    );

    @Query("""
    select count(p)
    from Product p
    where p.stock <= 0
    """)
    long countSoldOutProducts();

    @Query("""
    select count(p)
    from Product p
    where p.stock <= :threshold
      and p.stock > 0
    """)
    long countLowStockProducts(@Param("threshold") int threshold);

    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.product.dto.ProductResponse(
        p.id,
        p.name,
        p.description,
        p.price,
        p.initialStock,
        p.stock,
        p.soldCount,
        p.maxPurchaseQuantity,
        p.type,
        p.status,
        p.saleStartAt,
        p.saleEndAt
    )
    from Product p
    where p.status = :status
    """)
    List<ProductResponse> findAllProductsByStatus(ProductStatus status);





}
