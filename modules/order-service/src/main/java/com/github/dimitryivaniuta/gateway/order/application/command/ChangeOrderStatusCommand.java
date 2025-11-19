package com.github.dimitryivaniuta.gateway.order.application.command;

import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;

import java.util.UUID;

/**
 * Command for changing the status of an existing order.
 */
public record ChangeOrderStatusCommand(
        UUID orderId,
        OrderStatus status
) {
}
