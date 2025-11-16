package com.github.dimitryivaniuta.gateway.order.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    List<Order> findByIds(Collection<OrderId> ids);

    Order save(Order order);
}
