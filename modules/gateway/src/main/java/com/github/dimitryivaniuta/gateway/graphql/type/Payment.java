package com.github.dimitryivaniuta.gateway.graphql.type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Java representation of the GraphQL Payment type.
 */
public record Payment(
        UUID id,
        UUID orderId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String provider,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
