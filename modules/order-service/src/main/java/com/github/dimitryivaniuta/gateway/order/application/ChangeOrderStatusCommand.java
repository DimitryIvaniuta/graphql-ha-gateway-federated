package com.github.dimitryivaniuta.gateway.order.application;

import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;

import java.util.UUID;

public record ChangeOrderStatusCommand(
        UUID orderId,
        OrderStatus status
) { }
