package com.github.dimitryivaniuta.gateway.order.domain;

import com.github.dimitryivaniuta.gateway.common.money.Money;
import com.github.dimitryivaniuta.gateway.order.exception.DomainValidationException;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Order aggregate root (immutable value-object style).
 * <p>
 * Mutating operations (confirm/cancel/fulfill) return a new Order instance.
 */
@Builder
public record Order(
        OrderId id,
        UUID customerId,
        OrderStatus status,
        Money total,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String externalId
) {

    public Order {
        if (total == null) {
            throw new DomainValidationException("Total money must not be null");
        }
        if (status == null) {
            throw new DomainValidationException("Order status must not be null");
        }
        if (createdAt == null) {
            throw new DomainValidationException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new DomainValidationException("updatedAt must not be null");
        }
    }

    /**
     * Factory for a brand-new order (no id yet – assigned by persistence).
     */
    public static Order createNew(UUID customerId, Money total, String externalId) {
        if (total.isNegative()) {
            throw new DomainValidationException("Total amount must be non-negative");
        }
        OffsetDateTime now = OffsetDateTime.now();
        return new Order(
                null,               // id assigned by persistence
                customerId,
                OrderStatus.CREATED,
                total,
                now,
                now,
                externalId
        );
    }

    public boolean isCreated() {
        return status == OrderStatus.CREATED;
    }

    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean isFulfilled() {
        return status == OrderStatus.FULFILLED;
    }

    public Order confirm() {
        if (isCancelled()) {
            throw new DomainValidationException("Cannot confirm a cancelled order");
        }
        if (isFulfilled()) {
            throw new DomainValidationException("Cannot confirm a fulfilled order");
        }
        return withStatus(OrderStatus.CONFIRMED);
    }

    public Order cancel() {
        if (isFulfilled()) {
            throw new DomainValidationException("Cannot cancel a fulfilled order");
        }
        return withStatus(OrderStatus.CANCELLED);
    }

    public Order fulfill() {
        if (!isConfirmed()) {
            throw new DomainValidationException("Only confirmed orders can be fulfilled");
        }
        return withStatus(OrderStatus.FULFILLED);
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(
                this.id,
                this.customerId,
                newStatus,
                this.total,
                this.createdAt,
                OffsetDateTime.now(),
                this.externalId
        );
    }

    /**
     * Returns a copy with the persisted id set (used by persistence adapter).
     */
    public Order withId(OrderId newId) {
        return new Order(
                newId,
                this.customerId,
                this.status,
                this.total,
                this.createdAt,
                this.updatedAt,
                this.externalId
        );
    }
}

