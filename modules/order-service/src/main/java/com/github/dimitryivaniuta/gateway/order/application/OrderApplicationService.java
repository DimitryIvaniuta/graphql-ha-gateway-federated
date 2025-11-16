package com.github.dimitryivaniuta.gateway.order.application;

import com.github.dimitryivaniuta.gateway.order.domain.*;
import com.github.dimitryivaniuta.gateway.order.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderDomainService orderDomainService = new OrderDomainService();

    @Transactional
    public Order createOrder(CreateOrderCommand cmd) {
        Money total = new Money(cmd.totalAmount(), cmd.currency());
        Order domainOrder = orderDomainService.createNewOrder(cmd.customerId(), total, cmd.externalId());
        return orderRepository.save(domainOrder);
    }

    @Transactional
    public Order changeStatus(ChangeOrderStatusCommand cmd) {
        OrderId id = new OrderId(cmd.orderId());
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(cmd.orderId()));

        switch (cmd.status()) {
            case CONFIRMED -> order.confirm();
            case CANCELLED -> order.cancel();
            case FULFILLED -> order.fulfill();
            default -> { /* CREATED is initial */ }
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(new OrderId(id))
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Collection<UUID> ids) {
        List<OrderId> orderIds = ids.stream().map(OrderId::of).toList();
        return orderRepository.findByIds(orderIds);
    }
}
