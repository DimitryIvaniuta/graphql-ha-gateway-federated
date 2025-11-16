package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        String externalId,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
