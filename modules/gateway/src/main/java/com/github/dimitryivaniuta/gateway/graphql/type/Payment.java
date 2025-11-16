package com.github.dimitryivaniuta.gateway.graphql.type;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Java representation of the GraphQL Payment type.
 */
public record Payment(
        UUID id,
        UUID orderId,
        PaymentStatus status,
        MoneyDto total,
        String provider,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
