package com.github.dimitryivaniuta.gateway.order.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Order aggregate root (domain model, no JPA).
 */
public class Order {

    private final OrderId id;
    private final UUID customerId;
    private OrderStatus status;
    private final Money total;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private final String externalId;

    public Order(OrderId id,
                 UUID customerId,
                 OrderStatus status,
                 Money total,
                 OffsetDateTime createdAt,
                 OffsetDateTime updatedAt,
                 String externalId) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.externalId = externalId;
    }

    public OrderId id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public Money total() {
        return total;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime updatedAt() {
        return updatedAt;
    }

    public String externalId() {
        return externalId;
    }

    public void confirm() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled order");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (status == OrderStatus.FULFILLED) {
            throw new IllegalStateException("Cannot cancel a fulfilled order");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void fulfill() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed orders can be fulfilled");
        }
        this.status = OrderStatus.FULFILLED;
        this.updatedAt = OffsetDateTime.now();
    }
}
