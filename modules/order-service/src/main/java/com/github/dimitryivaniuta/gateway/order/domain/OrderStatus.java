package com.github.dimitryivaniuta.gateway.order.domain;

/**
 * Domain status of an order.
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    CANCELLED,
    FULFILLED
}
