package com.github.dimitryivaniuta.gateway.graphql.resolver.type;

import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Field resolvers for {@code Payment}.
 */
@Controller
public class PaymentTypeResolver {

    /**
     * Resolve Payment.order via "orderBatchLoader".
     */
    @SchemaMapping(typeName = "Payment", field = "order")
    public CompletableFuture<Order> order(Payment payment,
                                          DataLoader<UUID, Order> orderBatchLoader) {
        return orderBatchLoader.load(payment.orderId());
    }
}
