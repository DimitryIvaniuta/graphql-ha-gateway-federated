package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.mapper;

import com.github.dimitryivaniuta.gateway.order.domain.*;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;

import java.util.UUID;

public final class OrderEntityMapper {

    private OrderEntityMapper() { }

    public static Order toDomain(OrderJpaEntity entity) {
        OrderId id = entity.getId() != null ? new OrderId(entity.getId()) : null;
        Money total = new Money(entity.getTotalAmount(), entity.getCurrency());
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

    public static OrderJpaEntity toEntity(Order domain) {
        OrderJpaEntity entity = new OrderJpaEntity();
        if (domain.id() != null) {
            entity.setId(domain.id().value());
        }
        entity.setCustomerId(domain.customerId());
        entity.setStatus(domain.status());
        entity.setTotalAmount(domain.total().amount());
        entity.setCurrency(domain.total().currency());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setExternalId(domain.externalId());
        return entity;
    }

    public static UUID toUuid(OrderId id) {
        return id != null ? id.value() : null;
    }
}
