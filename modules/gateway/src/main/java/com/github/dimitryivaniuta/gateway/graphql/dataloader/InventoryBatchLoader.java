package com.github.dimitryivaniuta.gateway.graphql.dataloader;

import com.github.dimitryivaniuta.gateway.graphql.type.InventoryItem;
import org.dataloader.MappedBatchLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Batched loader for inventory items by order id.
 *
 * <p>Used by DataLoader "inventoryBatchLoader" for Order.inventoryItems.</p>
 */
@Component("inventoryBatchLoader")
public class InventoryBatchLoader implements MappedBatchLoader<UUID, List<InventoryItem>> {

    private final WebClient inventoryWebClient;

    public InventoryBatchLoader(@Qualifier("inventoryWebClient") WebClient inventoryWebClient) {
        this.inventoryWebClient = inventoryWebClient;
    }

    /**
     * Downstream contract: inventory-service returns an array of items with an "orderId" field.
     * We use a DTO here and then map to GraphQL InventoryItem + group by orderId.
     */
    private record InventoryItemByOrderDto(
            UUID orderId,
            UUID id,
            String sku,
            String name,
            String description,
            int availableQuantity,
            int reservedQuantity
    ) { }

    @Override
    public CompletableFuture<Map<UUID, List<InventoryItem>>> load(Set<UUID> keys) {
        if (keys == null || keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        String orderIdsParam = keys.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        return inventoryWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/inventory/by-orders")
                        .queryParam("orderIds", orderIdsParam)
                        .build())
                .retrieve()
                .bodyToFlux(InventoryItemByOrderDto.class)
                .collectList()
                .map(list -> {
                    Map<UUID, List<InventoryItem>> result = new HashMap<>();
                    for (InventoryItemByOrderDto dto : list) {
                        InventoryItem item = InventoryItem.builder()
                                .id(dto.id())
                                .name(dto.name())
                                .description(dto.description())
                                .sku(dto.sku())
                                .availableQuantity(dto.availableQuantity())
                                .reservedQuantity(dto.reservedQuantity())
                                .build();
                        result.computeIfAbsent(dto.orderId(), k -> new ArrayList<>())
                                .add(item);
                    }
                    // Ensure all requested keys exist in map (empty lists for missing)
                    keys.forEach(k -> result.computeIfAbsent(k, ignored -> List.of()));
                    return result;
                })
                .toFuture();
    }
}

