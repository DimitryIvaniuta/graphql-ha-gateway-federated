package com.github.dimitryivaniuta.gateway.graphql.type;

import java.math.BigDecimal;
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
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
