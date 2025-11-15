package com.github.dimitryivaniuta.gateway.persistence.service.client;

import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import com.github.dimitryivaniuta.gateway.graphql.type.input.CapturePaymentInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HTTP client for the Payment service used by the GraphQL gateway.
 *
 * REST contract:
 *  - GET  /internal/payments?ids=<comma-separated UUIDs>          -> [Payment]
 *  - GET  /internal/payments/by-orders?orderIds=<comma-separated> -> [Payment]
 *  - POST /internal/payments/capture                              -> Payment
 */
@Slf4j
@Service
public class PaymentClient {

    private static final String PAYMENTS_PATH = "/internal/payments";
    private static final String PAYMENTS_BY_ORDERS_PATH = "/internal/payments/by-orders";
    private static final String IDS_PARAM = "ids";
    private static final String ORDER_IDS_PARAM = "orderIds";

    private final WebClient paymentWebClient;

    public PaymentClient(@Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }

    // -------------------------------------------------------------------------
    // Fetch by payment IDs
    // -------------------------------------------------------------------------

    /**
     * Fetch payments by IDs, preserving the order of the input list.
     */
    public List<Payment> getPaymentsByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, Payment> byId = getPaymentsByIdsAsMap(new LinkedHashSet<>(ids));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Fetch payments by IDs and return as map keyed by payment ID.
     */
    public Map<UUID, Payment> getPaymentsByIdsAsMap(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        String idsParam = ids.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        log.debug("Fetching payments for ids={}", idsParam);

        List<Payment> fetched = paymentWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PAYMENTS_PATH)
                        .queryParam(IDS_PARAM, idsParam)
                        .build())
                .retrieve()
                .bodyToFlux(Payment.class)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);

        return fetched.stream()
                .collect(Collectors.toMap(
                        Payment::id,
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    // -------------------------------------------------------------------------
    // Fetch by order IDs (for DataLoader / Order.payments)
    // -------------------------------------------------------------------------

    /**
     * Fetch payments grouped by order ID.
     *
     * @param orderIds set of order IDs
     * @return map: orderId -> list of payments
     */
    public Map<UUID, List<Payment>> getPaymentsByOrderIds(Set<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String orderIdsParam = orderIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));

        log.debug("Fetching payments by orderIds={}", orderIdsParam);

        List<Payment> fetched = paymentWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PAYMENTS_BY_ORDERS_PATH)
                        .queryParam(ORDER_IDS_PARAM, orderIdsParam)
                        .build())
                .retrieve()
                .bodyToFlux(Payment.class)
                .collectList()
                .blockOptional()
                .orElseGet(List::of);

        Map<UUID, List<Payment>> result = new HashMap<>();

        for (Payment payment : fetched) {
            UUID orderId = payment.orderId();
            result.computeIfAbsent(orderId, k -> new ArrayList<>())
                    .add(payment);
        }

        // Ensure each requested orderId is present (possibly empty)
        orderIds.forEach(id -> result.computeIfAbsent(id, ignored -> List.of()));

        return result;
    }

    // -------------------------------------------------------------------------
    // Capture payment
    // -------------------------------------------------------------------------

    /**
     * Capture (finalize) a payment for an order.
     */
    public Payment capturePayment(CapturePaymentInput input) {
        log.debug("Capturing payment for orderId={}, amount={}, currency={}",
                input.orderId(), input.amount(), input.currency());

        return paymentWebClient.post()
                .uri(PAYMENTS_PATH + "/capture")
                .bodyValue(input)
                .retrieve()
                .bodyToMono(Payment.class)
                .block();
    }
}
