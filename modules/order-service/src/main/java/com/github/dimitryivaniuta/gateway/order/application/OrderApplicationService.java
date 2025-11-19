package com.github.dimitryivaniuta.gateway.order.application;

import com.github.dimitryivaniuta.gateway.common.money.Money;
import com.github.dimitryivaniuta.gateway.order.application.command.ChangeOrderStatusCommand;
import com.github.dimitryivaniuta.gateway.order.application.command.CreateOrderCommand;
import com.github.dimitryivaniuta.gateway.order.application.command.CreateOrderItemCommand;
import com.github.dimitryivaniuta.gateway.order.domain.*;
import com.github.dimitryivaniuta.gateway.order.exception.DomainValidationException;
import com.github.dimitryivaniuta.gateway.order.exception.OrderNotFoundException;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.mapper.OrderEntityMapper;
import com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.repository.SpringDataOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service orchestrating use-cases for the Order aggregate.
 * <p>
 * - Talks to domain via OrderRepository / OrderDomainService.
 * - Uses SpringDataOrderJpaRepository only for paging.
 */
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final SpringDataOrderJpaRepository jpaRepository;

    private final OrderDomainService domainService = new OrderDomainService();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Transactional
    public Order createOrder(CreateOrderCommand cmd) {
        Money total = calculateTotal(cmd.items());
        Order newOrder = domainService.createNewOrder(cmd.customerId(), total, cmd.externalId());
        return orderRepository.save(newOrder);
    }

    // -------------------------------------------------------------------------
    // CHANGE STATUS
    // -------------------------------------------------------------------------

    @Transactional
    public Order changeStatus(ChangeOrderStatusCommand cmd) {
        OrderId id = new OrderId(cmd.orderId());
        Order current = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(cmd.orderId()));

        Order updated;
        switch (cmd.status()) {
            case CONFIRMED -> updated = current.confirm();
            case CANCELLED -> updated = current.cancel();
            case FULFILLED -> updated = current.fulfill();
            case CREATED -> updated = current;
            default -> throw new DomainValidationException("Unsupported status: " + cmd.status());
        }

        return orderRepository.save(updated);
    }

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(new OrderId(id))
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<OrderId> orderIds = ids.stream()
                .map(OrderId::of)
                .toList();
        return orderRepository.findByIds(orderIds);
    }

    @Transactional(readOnly = true)
    public Page<Order> findPage(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderJpaEntity> jpaPage = jpaRepository.findAll(pageable);
        List<Order> content = jpaPage.getContent().stream()
                .map(OrderEntityMapper::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, jpaPage.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Money calculateTotal(List<CreateOrderItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new DomainValidationException("Order must contain at least one item");
        }

        String currency = null;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemCommand item : items) {
            if (item == null || item.unitPrice() == null) {
                throw new DomainValidationException("Item and unitPrice must not be null");
            }

            String itemCurrency = item.unitPrice().getCurrencyCode();
            if (currency == null) {
                currency = itemCurrency;
            } else if (!currency.equals(itemCurrency)) {
                throw new DomainValidationException("All items must have the same currency");
            }

            BigDecimal line = item.unitPrice().getAmount()
                    .multiply(BigDecimal.valueOf(item.quantity()));
            totalAmount = totalAmount.add(line);
        }

        return Money.of(totalAmount, currency);
    }
}
