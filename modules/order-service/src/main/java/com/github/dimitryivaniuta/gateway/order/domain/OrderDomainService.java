package com.github.dimitryivaniuta.gateway.order.domain;

import com.github.dimitryivaniuta.gateway.order.exception.DomainValidationException;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain service encapsulating business logic around Order creation.
 */
public class OrderDomainService {

    public Order createNewOrder(UUID customerId, Money total, String externalId) {
        if (total.amount().signum() < 0) {
            throw new DomainValidationException("Total amount must be non-negative");
        }
        OffsetDateTime now = OffsetDateTime.now();
        return new Order(
                null,  // ID assigned by persistence
                customerId,
                OrderStatus.CREATED,
                total,
                now,
                now,
                externalId
        );
    }
}
