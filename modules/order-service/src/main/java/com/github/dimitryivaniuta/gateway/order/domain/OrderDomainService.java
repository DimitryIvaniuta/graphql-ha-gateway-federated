package com.github.dimitryivaniuta.gateway.order.domain;

import com.github.dimitryivaniuta.gateway.common.money.Money;

/**
 * Domain service encapsulating higher-level business operations
 * around Order creation (invariants spanning multiple concerns).
 * <p>
 * Pure domain, no Spring or persistence here.
 */
public class OrderDomainService {

    /**
     * Create a new order aggregate, delegating to {@link Order#createNew}.
     */
    public Order createNewOrder(java.util.UUID customerId,
                                Money total,
                                String externalId) {
        return Order.createNew(customerId, total, externalId);
    }
}
