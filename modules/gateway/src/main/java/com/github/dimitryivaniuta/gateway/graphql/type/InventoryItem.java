package com.github.dimitryivaniuta.gateway.graphql.type;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Java representation of the GraphQL InventoryItem type.
 */
@Builder
public record InventoryItem(
        UUID id,
        String sku,
        String name,
        String description,
        int availableQuantity,
        int reservedQuantity,
        OffsetDateTime updatedAt
) {
}
