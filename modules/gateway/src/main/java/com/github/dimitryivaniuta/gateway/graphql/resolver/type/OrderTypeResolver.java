package com.github.dimitryivaniuta.gateway.graphql.resolver.type;

import com.github.dimitryivaniuta.gateway.graphql.type.InventoryItem;
import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Field resolvers for the {@code Order} GraphQL type.
 */
@Controller
public class OrderTypeResolver {

    /**
     * Resolve Order.inventoryItems via DataLoader "inventoryBatchLoader".
     */
    @SchemaMapping(typeName = "Order", field = "inventoryItems")
    public CompletableFuture<List<InventoryItem>> inventoryItems(Order order,
                                                                 DataLoader<UUID, List<InventoryItem>> inventoryBatchLoader) {
        // The actual key shape depends on your schema; here we simply use order ID.
        return inventoryBatchLoader.load(order.id());
    }

    /**
     * Resolve Order.payments via DataLoader "paymentBatchLoader".
     */
    @SchemaMapping(typeName = "Order", field = "payments")
    public CompletableFuture<List<Payment>> payments(Order order,
                                                     DataLoader<UUID, List<Payment>> paymentBatchLoader) {
        return paymentBatchLoader.load(order.id());
    }
}
