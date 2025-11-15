package com.github.dimitryivaniuta.gateway.graphql.type;

/**
 * Java enum backing the GraphQL PaymentStatus.
 */
public enum PaymentStatus {
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    REFUNDED
}