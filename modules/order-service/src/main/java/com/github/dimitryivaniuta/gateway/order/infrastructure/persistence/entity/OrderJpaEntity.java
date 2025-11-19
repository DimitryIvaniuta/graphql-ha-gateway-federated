package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity;

import com.github.dimitryivaniuta.gateway.common.persistence.AbstractUuidEntity;
import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to order_service.order_header.
 */
@Entity
@Table(
        name = "order_header",
        schema = "order_service",
        indexes = {
                @Index(name = "idx_order_header_customer_id", columnList = "customer_id"),
                @Index(name = "idx_order_header_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class OrderJpaEntity extends AbstractUuidEntity {

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "customer_id", columnDefinition = "uuid")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private OrderStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "amount",
                    column = @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
            ),
            @AttributeOverride(
                    name = "currency",
                    column = @Column(name = "currency", length = 3, nullable = false)
            )
    })
    private MoneyEmbeddable total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = OrderStatus.CREATED;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
