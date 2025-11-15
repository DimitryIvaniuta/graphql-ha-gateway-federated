package com.github.dimitryivaniuta.gateway.graphql.resolver.type;

import com.github.dimitryivaniuta.gateway.graphql.type.InventoryItem;
import com.github.dimitryivaniuta.gateway.graphql.type.Order;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Field resolvers for {@code InventoryItem}.
 */
@Controller
public class InventoryItemTypeResolver {

    /**
     * Resolve InventoryItem.orders via DataLoader "orderBatchLoader".
     */
    @SchemaMapping(typeName = "InventoryItem", field = "orders")
    public CompletableFuture<List<Order>> orders(InventoryItem item,
                                                 DataLoader<UUID, List<Order>> orderBatchLoader) {
        return orderBatchLoader.load(item.id());
    }
}
