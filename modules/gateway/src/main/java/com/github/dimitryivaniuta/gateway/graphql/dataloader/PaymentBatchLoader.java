package com.github.dimitryivaniuta.gateway.graphql.dataloader;

import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import org.dataloader.MappedBatchLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Batched loader for payments by order id.
 *
 * <p>Used by DataLoader "paymentBatchLoader" for Order.payments.</p>
 */
@Component("paymentBatchLoader")
public class PaymentBatchLoader implements MappedBatchLoader<UUID, List<Payment>> {

    private final WebClient paymentWebClient;

    public PaymentBatchLoader(@Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }

    @Override
    public CompletableFuture<Map<UUID, List<Payment>>> load(Set<UUID> keys) {
        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        String orderIdsParam = keys.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        return paymentWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/payments/by-orders")
                        .queryParam("orderIds", orderIdsParam)
                        .build())
                .retrieve()
                .bodyToFlux(Payment.class)
                .collectList()
                .map(list -> {
                    Map<UUID, List<Payment>> result = new HashMap<>();
                    for (Payment payment : list) {
                        UUID orderId = payment.orderId();
                        result.computeIfAbsent(orderId, k -> new ArrayList<>())
                                .add(payment);
                    }
                    // Ensure all requested keys present
                    keys.forEach(k -> result.computeIfAbsent(k, ignored -> List.of()));
                    return result;
                })
                .toFuture();
    }
}
