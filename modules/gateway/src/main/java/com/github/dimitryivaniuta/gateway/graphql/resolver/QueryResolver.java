package com.github.dimitryivaniuta.gateway.graphql.resolver;

import com.github.dimitryivaniuta.gateway.graphql.type.InventoryItem;
import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Root GraphQL queries for the gateway.
 *
 * <p>Delegates to downstream domain services (order, inventory, payment)
 * via WebClient and maps responses to GraphQL types.</p>
 */
@Controller
public class QueryResolver {

    private static final Logger log = LoggerFactory.getLogger(QueryResolver.class);

    private final WebClient orderWebClient;
    private final WebClient inventoryWebClient;
    private final WebClient paymentWebClient;

    public QueryResolver(@Qualifier("orderWebClient") WebClient orderWebClient,
                         @Qualifier("inventoryWebClient") WebClient inventoryWebClient,
                         @Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.orderWebClient = orderWebClient;
        this.inventoryWebClient = inventoryWebClient;
        this.paymentWebClient = paymentWebClient;
    }

    /**
     * Query: orders(ids: [ID!]!): [Order!]!
     */
    @QueryMapping
    public List<Order> orders(@Argument List<UUID> ids) {
        return fetchAndOrderByIds(
                orderWebClient,
                "/internal/orders",
                "ids",
                ids,
                Order.class,
                Order::id
        );
    }

    /**
     * Query: inventoryItems(ids: [ID!]!): [InventoryItem!]!
     */
    @QueryMapping
    public List<InventoryItem> inventoryItems(@Argument List<UUID> ids) {
        return fetchAndOrderByIds(
                inventoryWebClient,
                "/internal/inventory",
                "ids",
                ids,
                InventoryItem.class,
                InventoryItem::id
        );
    }

    /**
     * Query: payments(ids: [ID!]!): [Payment!]!
     */
    @QueryMapping
    public List<Payment> payments(@Argument List<UUID> ids) {
        return fetchAndOrderByIds(
                paymentWebClient,
                "/internal/payments",
                "ids",
                ids,
                Payment.class,
                Payment::id
        );
    }

    /**
     * Helper that:
     * <ul>
     *   <li>Makes a single GET call with comma-separated IDs.</li>
     *   <li>Maps the response to type T.</li>
     *   <li>Reorders the result list to match the original ID order.</li>
     * </ul>
     */
    private <T> List<T> fetchAndOrderByIds(WebClient client,
                                           String path,
                                           String paramName,
                                           List<UUID> ids,
                                           Class<T> elementType,
                                           Function<T, UUID> idExtractor) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String idsParam = ids.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        log.debug("Fetching {} for {}={}", elementType.getSimpleName(), paramName, idsParam);

        List<T> fetched = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam(paramName, idsParam)
                        .build())
                .retrieve()
                .bodyToFlux(elementType)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);

        Map<UUID, T> byId = fetched.stream()
                .collect(Collectors.toMap(
                        idExtractor,
                        Function.identity(),
                        (a, b) -> a // should not happen if IDs are unique
                ));

        // Preserve request order and drop unknown IDs gracefully.
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
