package com.github.dimitryivaniuta.gateway.order.exception;

import java.util.UUID;

/**
 * Thrown when an order is not found.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order not found: " + id);
    }
}