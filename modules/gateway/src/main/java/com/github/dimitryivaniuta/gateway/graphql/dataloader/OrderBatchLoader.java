package com.github.dimitryivaniuta.gateway.graphql.dataloader;

import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import org.dataloader.MappedBatchLoader;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Batched loader for Orders by id.
 *
 * <p>Used by DataLoader "orderBatchLoader", e.g. for Payment.order field resolution.</p>
 */
@Component("orderBatchLoader")
public class OrderBatchLoader implements MappedBatchLoader<UUID, Order> {

    private final WebClient orderWebClient;

    public OrderBatchLoader(@Qualifier("orderWebClient") WebClient orderWebClient) {
        this.orderWebClient = orderWebClient;
    }

    @NotNull
    @Override
    public CompletableFuture<Map<UUID, Order>> load(Set<UUID> keys) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        String idsParam = keys.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        return orderWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/orders")
                        .queryParam("ids", idsParam)
                        .build())
                .retrieve()
                .bodyToFlux(Order.class)
                .collectList()
                .map(orders -> orders.stream()
                        .collect(
                                Collectors.toMap(
                                        Order::id,
                                        Function.identity()
                                )
                        )
                )
                .toFuture();
    }
}
