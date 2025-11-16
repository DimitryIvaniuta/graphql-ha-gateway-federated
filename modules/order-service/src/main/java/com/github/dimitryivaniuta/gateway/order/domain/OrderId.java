package com.github.dimitryivaniuta.gateway.order.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed ID wrapper for Orders.
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }
}
