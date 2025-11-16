package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;

import java.util.UUID;

/**
 * Request DTO for a single order line when creating an order.
 */
public record CreateOrderItemRequestDto(
        UUID inventoryItemId,
        int quantity,
        MoneyDto total
) { }
