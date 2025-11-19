package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.mapper;

import com.github.dimitryivaniuta.gateway.common.money.Money;
import com.github.dimitryivaniuta.gateway.order.domain.*;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.MoneyEmbeddable;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps between domain Order aggregate and JPA entity.
 */
public final class OrderEntityMapper {

    private OrderEntityMapper() {
    }

    public static Order toDomain(OrderJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        OrderId id = entity.getId() != null ? new OrderId(entity.getId()) : null;
        Money total = entity.getTotal() != null
                ? entity.getTotal().toMoney()
                : Money.of(BigDecimal.ZERO, "USD");

        return new Order(
                id,
                entity.getCustomerId(),
                entity.getStatus(),
                total,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getExternalId()
        );
    }

    public static OrderJpaEntity toEntity(Order order) {
        if (order == null) {
            return null;
        }

        UUID id = order.id() != null ? order.id().value() : null;
        OffsetDateTime createdAt = order.createdAt();
        OffsetDateTime updatedAt = order.updatedAt();

        Money total = order.total();

        MoneyEmbeddable totalEmbeddable = MoneyEmbeddable.fromMoney(total);

        return OrderJpaEntity.builder()
                .id(id)
                .externalId(order.externalId())
                .customerId(order.customerId())
                .status(order.status())
                .total(totalEmbeddable)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static UUID toUuid(OrderId id) {
        return id != null ? id.value() : null;
    }
}
