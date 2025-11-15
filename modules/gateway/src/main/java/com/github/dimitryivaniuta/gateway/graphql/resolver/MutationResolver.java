package com.github.dimitryivaniuta.gateway.graphql.resolver;

import com.github.dimitryivaniuta.gateway.graphql.type.InventoryItem;
import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import com.github.dimitryivaniuta.gateway.graphql.type.input.CapturePaymentInput;
import com.github.dimitryivaniuta.gateway.graphql.type.input.CreateOrderInput;
import com.github.dimitryivaniuta.gateway.graphql.type.input.UpdateInventoryItemInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Root GraphQL mutations for the gateway.
 *
 * <p>Each mutation delegates to the corresponding domain service
 * and returns the created/updated resource.</p>
 */
@Controller
@Slf4j
public class MutationResolver {

    private final WebClient orderWebClient;
    private final WebClient inventoryWebClient;
    private final WebClient paymentWebClient;

    public MutationResolver(@Qualifier("orderWebClient") WebClient orderWebClient,
                            @Qualifier("inventoryWebClient") WebClient inventoryWebClient,
                            @Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.orderWebClient = orderWebClient;
        this.inventoryWebClient = inventoryWebClient;
        this.paymentWebClient = paymentWebClient;
    }

    /**
     * Mutation: createOrder(input: CreateOrderInput!): Order!
     */
    @MutationMapping
    public Order createOrder(@Argument CreateOrderInput input) {
        log.debug("Creating order for customerId={}, items={}",
                input.customerId(), input.items() != null ? input.items().size() : 0);

        return orderWebClient.post()
                .uri("/internal/orders")
                .bodyValue(input)
                .retrieve()
                .bodyToMono(Order.class)
                .block();
    }

    /**
     * Mutation: updateInventoryItem(input: UpdateInventoryItemInput!): InventoryItem!
     */
    @MutationMapping
    public InventoryItem updateInventoryItem(@Argument UpdateInventoryItemInput input) {
        log.debug("Updating inventory item id={}, availableQuantity={}, reservedQuantity={}",
                input.id(), input.availableQuantity(), input.reservedQuantity());

        return inventoryWebClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/inventory/{id}")
                        .build(input.id()))
                .bodyValue(input)
                .retrieve()
                .bodyToMono(InventoryItem.class)
                .block();
    }

    /**
     * Mutation: capturePayment(input: CapturePaymentInput!): Payment!
     */
    @MutationMapping
    public Payment capturePayment(@Argument CapturePaymentInput input) {
        log.debug("Capturing payment for orderId={}, amount={}, currency={}",
                input.orderId(), input.amount(), input.currency());

        return paymentWebClient.post()
                .uri("/internal/payments/capture")
                .bodyValue(input)
                .retrieve()
                .bodyToMono(Payment.class)
                .block();
    }
}
