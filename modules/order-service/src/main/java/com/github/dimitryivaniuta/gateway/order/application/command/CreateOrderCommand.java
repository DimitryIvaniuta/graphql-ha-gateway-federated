package com.github.dimitryivaniuta.gateway.order.application.command;

import java.util.List;
import java.util.UUID;

/**
 * Command for creating a new order.
 */
public record CreateOrderCommand(
        UUID customerId,
        String externalId,
        List<CreateOrderItemCommand> items
) {
}
