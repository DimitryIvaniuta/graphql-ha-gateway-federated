package com.github.dimitryivaniuta.gateway.graphql.type.input;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;

import java.util.List;
import java.util.UUID;

/**
 * Java representation of the GraphQL CreateOrderInput.
 */
public record CreateOrderInput(
        String clientMutationId,
        UUID customerId,
        List<CreateOrderItemInput> items,
        MoneyDto total
) {
}
