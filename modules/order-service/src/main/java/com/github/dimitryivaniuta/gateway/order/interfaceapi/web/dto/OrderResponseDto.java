package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;
import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Order representation exposed via HTTP.
 */
public record OrderResponseDto(
        UUID id,
        String externalId,
        UUID customerId,
        OrderStatus status,
        MoneyDto total,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
