package com.github.dimitryivaniuta.gateway.graphql.type.input;

import java.util.UUID;

/**
 * Java representation of the GraphQL UpdateInventoryItemInput.
 *
 * Both quantities are nullable to support partial updates.
 */
public record UpdateInventoryItemInput(
        UUID id,
        Integer availableQuantity,
        Integer reservedQuantity
) {
}
