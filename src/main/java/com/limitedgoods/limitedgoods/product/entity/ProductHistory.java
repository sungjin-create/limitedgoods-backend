package com.limitedgoods.limitedgoods.product.entity;

import com.limitedgoods.limitedgoods.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
@Table(name = "product_history")
public class ProductHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "history_type", nullable = false, length = 30)
    private ProductHistoryType historyType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", nullable = false)
    private Map<String, ProductFieldChange> changes;

    // 상태와 재고는 조회 조건으로 자주 사용될 수 있으므로 정형 컬럼을 유지한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ProductStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private ProductStatus toStatus;

    @Column(name = "from_stock")
    private Integer fromStock;

    @Column(name = "to_stock")
    private Integer toStock;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ProductHistory(
            Product product,
            User changedByUser,
            ProductHistoryType historyType,
            Map<String, ProductFieldChange> changes,
            ProductStatus fromStatus,
            ProductStatus toStatus,
            Integer fromStock,
            Integer toStock,
            String reason
    ) {
        this.product = product;
        this.changedByUser = changedByUser;
        this.historyType = historyType;
        this.changes = Map.copyOf(changes);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.fromStock = fromStock;
        this.toStock = toStock;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public static ProductHistory create(
            Product product,
            User changedByUser,
            ProductHistoryType historyType,
            Map<String, ProductFieldChange> changes,
            ProductStatus fromStatus,
            ProductStatus toStatus,
            Integer fromStock,
            Integer toStock,
            String reason
    ) {
        return new ProductHistory(
                product,
                changedByUser,
                historyType,
                changes,
                fromStatus,
                toStatus,
                fromStock,
                toStock,
                reason
        );
    }
}
