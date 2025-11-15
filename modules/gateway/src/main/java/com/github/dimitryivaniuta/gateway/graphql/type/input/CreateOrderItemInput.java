package com.github.dimitryivaniuta.gateway.graphql.type.input;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Java representation of the GraphQL CreateOrderItemInput.
 */
public record CreateOrderItemInput(
        UUID inventoryItemId,
        int quantity,
        BigDecimal unitPrice,
        String currency
) {
}
