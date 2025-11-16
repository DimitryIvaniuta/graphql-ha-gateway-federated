package com.github.dimitryivaniuta.gateway.order.interfaceapi.web;

import com.github.dimitryivaniuta.gateway.order.application.ChangeOrderStatusCommand;
import com.github.dimitryivaniuta.gateway.order.application.CreateOrderCommand;
import com.github.dimitryivaniuta.gateway.order.application.OrderApplicationService;
import com.github.dimitryivaniuta.gateway.order.domain.Order;
import com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto.ChangeOrderStatusRequestDto;
import com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto.CreateOrderRequestDto;
import com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto.OrderResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Internal REST API for order-service, consumed by the GraphQL gateway.
 */
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderService;

    @GetMapping
    public List<OrderResponseDto> getByIds(@RequestParam("ids") String idsParam) {
        List<UUID> ids = Arrays.stream(idsParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();

        List<Order> orders = orderService.getOrders(ids);
        Map<UUID, Order> byId = orders.stream()
                .collect(Collectors.toMap(o -> o.id().value(), o -> o));

        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@Valid @RequestBody CreateOrderRequestDto body) {
        CreateOrderCommand cmd = new CreateOrderCommand(
                body.customerId(),
                body.totalAmount(),
                body.currency(),
                body.externalId()
        );
        Order order = orderService.createOrder(cmd);
        return ResponseEntity.ok(toDto(order));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> changeStatus(@PathVariable("id") UUID id,
                                                         @Valid @RequestBody ChangeOrderStatusRequestDto body) {
        ChangeOrderStatusCommand cmd = new ChangeOrderStatusCommand(id, body.status());
        Order updated = orderService.changeStatus(cmd);
        return ResponseEntity.ok(toDto(updated));
    }

    private OrderResponseDto toDto(Order order) {
        return new OrderResponseDto(
                order.id() != null ? order.id().value() : null,
                order.externalId(),
                order.customerId(),
                order.status(),
                order.total().amount(),
                order.total().currency(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}
