package com.github.dimitryivaniuta.gateway.order.application.command;

import com.github.dimitryivaniuta.gateway.common.money.Money;

/**
 * Single order line in the create-order use case.
 */
public record CreateOrderItemCommand(
        String sku,
        int quantity,
        Money unitPrice
) {
}
