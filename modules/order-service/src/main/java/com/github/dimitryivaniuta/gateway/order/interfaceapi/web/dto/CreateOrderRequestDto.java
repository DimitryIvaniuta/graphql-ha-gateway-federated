package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating an order.
 */
public record CreateOrderRequestDto(
        UUID customerId,
        String externalId,
        @NotEmpty @Valid List<CreateOrderItemRequestDto> items
) {
}
