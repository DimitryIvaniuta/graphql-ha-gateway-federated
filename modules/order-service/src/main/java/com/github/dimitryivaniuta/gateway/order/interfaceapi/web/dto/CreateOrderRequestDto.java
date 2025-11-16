package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequestDto(
        UUID customerId,
        @NotNull @PositiveOrZero BigDecimal totalAmount,
        @NotBlank String currency,
        String externalId
) { }
