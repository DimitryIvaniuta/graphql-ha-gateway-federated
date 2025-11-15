package com.github.dimitryivaniuta.gateway.persistence.service.client;

import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import com.github.dimitryivaniuta.gateway.graphql.type.input.CreateOrderInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HTTP client for the Order service used by the GraphQL gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch orders by IDs (list + map variants).</li>
 *   <li>Create orders via the order service.</li>
 * </ul>
 *
 * <p>Current REST contract:
 * <ul>
 *   <li>GET  /internal/orders?ids=&lt;comma-separated UUIDs&gt; → [Order]</li>
 *   <li>POST /internal/orders (body = CreateOrderInput) → Order</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderClient {

    private static final String ORDERS_PATH = "/internal/orders";
    private static final String IDS_PARAM = "ids";

    @Qualifier("orderWebClient")
    private final WebClient orderWebClient;

    /**
     * Fetch orders by IDs, preserving the original order of the input list.
     *
     * @param ids list of order IDs
     * @return list of orders in the same order as requested (unknown IDs are dropped)
     */
    public List<Order> getOrdersByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, Order> byId = getOrdersByIdsAsMap(new LinkedHashSet<>(ids));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Fetch orders by IDs and return them as a map keyed by ID.
     *
     * @param ids set of order IDs
     * @return map of ID -> Order
     */
    public Map<UUID, Order> getOrdersByIdsAsMap(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        String idsParam = ids.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        log.debug("Fetching orders for ids={}", idsParam);

        List<Order> fetched = orderWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(ORDERS_PATH)
                        .queryParam(IDS_PARAM, idsParam)
                        .build())
                .retrieve()
                .bodyToFlux(Order.class)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);

        return fetched.stream()
                .collect(Collectors.toMap(
                        Order::id,
                        Function.identity(),
                        (a, b) -> a // should not happen if IDs are unique
                ));
    }

    /**
     * Create a new order in the order service.
     *
     * @param input create-order payload
     * @return created Order
     */
    public Order createOrder(CreateOrderInput input) {
        log.debug("Creating order for customerId={}, items={}",
                input.customerId(),
                input.items() != null ? input.items().size() : 0);

        return orderWebClient.post()
                .uri(ORDERS_PATH)
                .bodyValue(input)
                .retrieve()
                .bodyToMono(Order.class)
                .block();
    }
}
