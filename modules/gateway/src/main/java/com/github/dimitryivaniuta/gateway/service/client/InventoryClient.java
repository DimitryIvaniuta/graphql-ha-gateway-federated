package com.github.dimitryivaniuta.gateway.service.client;

import com.github.dimitryivaniuta.gateway.graphql.type.InventoryItem;
import com.github.dimitryivaniuta.gateway.graphql.type.input.UpdateInventoryItemInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HTTP client for the Inventory service used by the GraphQL gateway.
 *
 * REST contract (can be adapted later if needed):
 *  - GET  /internal/inventory?ids=<comma-separated UUIDs>          -> [InventoryItem]
 *  - GET  /internal/inventory/by-orders?orderIds=<comma-separated> -> [InventoryItemByOrderDto]
 *  - PUT  /internal/inventory/{id}                                 -> InventoryItem
 */
@Slf4j
@Service
public class InventoryClient {

    private static final String INVENTORY_PATH = "/internal/inventory";
    private static final String INVENTORY_BY_ORDERS_PATH = "/internal/inventory/by-orders";
    private static final String IDS_PARAM = "ids";
    private static final String ORDER_IDS_PARAM = "orderIds";

    private final WebClient inventoryWebClient;

    public InventoryClient(@Qualifier("inventoryWebClient") WebClient inventoryWebClient) {
        this.inventoryWebClient = inventoryWebClient;
    }

    /**
     * DTO used when fetching inventory items by order IDs.
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

    // -------------------------------------------------------------------------
    // Fetch by item IDs
    // -------------------------------------------------------------------------

    /**
     * Fetch inventory items by IDs, preserving the order of the input list.
     */
    public List<InventoryItem> getInventoryItemsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, InventoryItem> byId = getInventoryItemsByIdsAsMap(new LinkedHashSet<>(ids));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Fetch inventory items by IDs and return as map keyed by ID.
     */
    public Map<UUID, InventoryItem> getInventoryItemsByIdsAsMap(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        String idsParam = ids.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        log.debug("Fetching inventory items for ids={}", idsParam);

        List<InventoryItem> fetched = inventoryWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(INVENTORY_PATH)
                        .queryParam(IDS_PARAM, idsParam)
                        .build())
                .retrieve()
                .bodyToFlux(InventoryItem.class)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);

        return fetched.stream()
                .collect(Collectors.toMap(
                        InventoryItem::id,
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    // -------------------------------------------------------------------------
    // Fetch by order IDs (for DataLoader)
    // -------------------------------------------------------------------------

    /**
     * Fetch inventory items grouped by order ID.
     *
     * @param orderIds set of order IDs
     * @return map: orderId -> list of InventoryItem
     */
    public Map<UUID, List<InventoryItem>> getInventoryItemsByOrderIds(Set<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String orderIdsParam = orderIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        log.debug("Fetching inventory items by orderIds={}", orderIdsParam);

        List<InventoryItemByOrderDto> dtos = inventoryWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(INVENTORY_BY_ORDERS_PATH)
                        .queryParam(ORDER_IDS_PARAM, orderIdsParam)
                        .build())
                .retrieve()
                .bodyToFlux(InventoryItemByOrderDto.class)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);

        Map<UUID, List<InventoryItem>> result = new HashMap<>();

        for (InventoryItemByOrderDto dto : dtos) {
            InventoryItem item = new InventoryItem(
                    dto.id(),
                    dto.sku(),
                    dto.name(),
                    dto.description(),
                    dto.availableQuantity(),
                    dto.reservedQuantity(),
                    null // updatedAt – fill if backend returns it
            );
            result.computeIfAbsent(dto.orderId(), k -> new ArrayList<>())
                    .add(item);
        }

        // Ensure each requested orderId is present (possibly with empty list)
        orderIds.forEach(id -> result.computeIfAbsent(id, ignored -> List.of()));

        return result;
    }

    // -------------------------------------------------------------------------
    // Update item
    // -------------------------------------------------------------------------

    /**
     * Update an inventory item (quantities, etc.).
     */
    public InventoryItem updateInventoryItem(UpdateInventoryItemInput input) {
        log.debug("Updating inventory item id={}, availableQuantity={}, reservedQuantity={}",
                input.id(), input.availableQuantity(), input.reservedQuantity());

        return inventoryWebClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path(INVENTORY_PATH + "/{id}")
                        .build(input.id()))
                .bodyValue(input)
                .retrieve()
                .bodyToMono(InventoryItem.class)
                .block();
    }
}
