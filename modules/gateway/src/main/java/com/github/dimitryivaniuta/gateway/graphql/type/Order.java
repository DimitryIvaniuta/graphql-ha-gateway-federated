package com.github.dimitryivaniuta.gateway.graphql.type;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Java representation of the GraphQL Order type.
 *
 * Corresponds to order.graphqls:
 * type Order { ... }
 */
public record Order(
        UUID id,
        String externalId,
        OrderStatus status,
        UUID customerId,
        MoneyDto total,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
