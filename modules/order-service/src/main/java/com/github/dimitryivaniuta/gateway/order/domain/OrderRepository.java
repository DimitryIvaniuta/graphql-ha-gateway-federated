package com.github.dimitryivaniuta.gateway.order.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository port for Order aggregate.
 * <p>
 * Implemented by infrastructure (OrderRepositoryJpaAdapter).
 * Used directly by OrderApplicationService as the persistence port.
 */
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    List<Order> findByIds(Collection<OrderId> ids);

    Order save(Order order);
}
