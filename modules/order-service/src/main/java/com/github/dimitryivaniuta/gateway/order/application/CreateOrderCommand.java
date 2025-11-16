package com.github.dimitryivaniuta.gateway.order.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        BigDecimal totalAmount,
        String currency,
        String externalId
) { }
