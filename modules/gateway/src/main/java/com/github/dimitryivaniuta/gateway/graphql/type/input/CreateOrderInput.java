package com.github.dimitryivaniuta.gateway.graphql.type.input;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Java representation of the GraphQL CreateOrderInput.
 */
public record CreateOrderInput(
        String clientMutationId,
        UUID customerId,
        List<CreateOrderItemInput> items,
        BigDecimal totalAmount,
        String currency
) {
}
