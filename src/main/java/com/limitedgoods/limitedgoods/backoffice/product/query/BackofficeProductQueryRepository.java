package com.limitedgoods.limitedgoods.backoffice.product.query;

import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductOrderSummaryQueryResult;
import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductResponse;
import com.limitedgoods.limitedgoods.backoffice.product.dto.ProductSummaryResponse;
import com.limitedgoods.limitedgoods.product.entity.Product;
import com.limitedgoods.limitedgoods.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BackofficeProductQueryRepository extends JpaRepository<Product, Long> {
    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.product.dto.ProductSummaryResponse(
        count(p),
        coalesce(sum(case when p.stock > 0 and p.stock <= 5 then 1 else 0 end), 0),
        coalesce(sum(case when p.stock = 0 then 1 else 0 end), 0)
    )
    from Product p
    """)
    ProductSummaryResponse findProductSummary();

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
    order by p.id
    """)
    List<ProductResponse> findAllProducts();

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
    order by p.id
    """)
    List<ProductResponse> findAllProductsByStatus(ProductStatus status);

    @Query("""
    select new com.limitedgoods.limitedgoods.backoffice.product.dto.ProductOrderSummaryQueryResult(
        p.stock,
        coalesce(
            sum(
                case
                    when o.status = com.limitedgoods.limitedgoods.order.entity.OrderStatus.CREATED
                    then oi.quantity
                    else 0
                end
            ),
            0
        ),
        coalesce(
            sum(
                case
                    when o.status in (
                        com.limitedgoods.limitedgoods.order.entity.OrderStatus.PAYMENT_PENDING,
                        com.limitedgoods.limitedgoods.order.entity.OrderStatus.PAYMENT_APPROVED
                    )
                    then oi.quantity
                    else 0
                end
            ),
            0
        )
    )
    from Product p
    left join OrderItem oi
        on oi.product.id = p.id
    left join Order o
        on o.id = oi.order.id
    where p.id = :productId
    group by p.id, p.stock
    """)
    Optional<ProductOrderSummaryQueryResult> findProductOrdersSummary(@Param("productId") Long productId);

}
