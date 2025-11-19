package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Single order line in create-order request.
 */
public record CreateOrderItemRequestDto(
        @NotBlank String sku,
        @Min(1) int quantity,
        @NotNull MoneyDto price
) {
}
