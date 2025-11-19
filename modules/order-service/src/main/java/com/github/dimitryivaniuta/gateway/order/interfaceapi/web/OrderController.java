package com.github.dimitryivaniuta.gateway.order.interfaceapi.web;

import com.github.dimitryivaniuta.gateway.common.money.Money;
import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;
import com.github.dimitryivaniuta.gateway.order.application.OrderApplicationService;
import com.github.dimitryivaniuta.gateway.order.application.command.ChangeOrderStatusCommand;
import com.github.dimitryivaniuta.gateway.order.application.command.CreateOrderCommand;
import com.github.dimitryivaniuta.gateway.order.application.command.CreateOrderItemCommand;
import com.github.dimitryivaniuta.gateway.order.domain.Order;
import com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HTTP API for order-service.
 * <p>
 * Exposes:
 * - GET  /internal/orders?ids=...        (for GraphQL gateway)
 * - POST /internal/orders                (create order)
 * - PATCH /internal/orders/{id}/status   (change status)
 * - GET  /api/orders?page=&size=         (paged listing for backoffice/UI)
 */
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderService;

    // -------------------------------------------------------------------------
    // Internal API for gateway
    // -------------------------------------------------------------------------

    @GetMapping(path = "/internal/orders", params = "ids")
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
                .map(this::toResponseDto)
                .toList();
    }

    @PostMapping("/internal/orders")
    public ResponseEntity<OrderResponseDto> createInternal(@Valid @RequestBody CreateOrderRequestDto body) {
        CreateOrderCommand cmd = toCreateCommand(body);
        Order order = orderService.createOrder(cmd);
        return ResponseEntity.ok(toResponseDto(order));
    }

    @PatchMapping("/internal/orders/{id}/status")
    public ResponseEntity<OrderResponseDto> changeStatusInternal(@PathVariable("id") UUID id,
                                                                 @Valid @RequestBody ChangeOrderStatusRequestDto body) {
        ChangeOrderStatusCommand cmd = new ChangeOrderStatusCommand(id, body.status());
        Order order = orderService.changeStatus(cmd);
        return ResponseEntity.ok(toResponseDto(order));
    }

    // -------------------------------------------------------------------------
    // Public API with paging (for UI/backoffice)
    // -------------------------------------------------------------------------

    @GetMapping(path = "/api/orders", params = {"page", "size"})
    public OrderPageResponseDto findPage(@RequestParam("page") int page,
                                         @RequestParam("size") int size) {
        Page<Order> resultPage = orderService.findPage(page, size);

        List<OrderResponseDto> content = resultPage.getContent().stream()
                .map(this::toResponseDto)
                .toList();

        return new OrderPageResponseDto(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private CreateOrderCommand toCreateCommand(CreateOrderRequestDto dto) {
        List<CreateOrderItemCommand> items = dto.items().stream()
                .map(this::toCreateItemCommand)
                .toList();

        return new CreateOrderCommand(
                dto.customerId(),
                dto.externalId(),
                items
        );
    }

    private CreateOrderItemCommand toCreateItemCommand(CreateOrderItemRequestDto dto) {
        Money price = toMoney(dto.price());
        return new CreateOrderItemCommand(
                dto.sku(),
                dto.quantity(),
                price
        );
    }

    private OrderResponseDto toResponseDto(Order order) {
        MoneyDto totalDto = toMoneyDto(order.total());
        return new OrderResponseDto(
                order.id().value(),
                order.externalId(),
                order.customerId(),
                order.status(),
                totalDto,
                order.createdAt(),
                order.updatedAt()
        );
    }

    private Money toMoney(MoneyDto dto) {
        if (dto == null) {
            return Money.of(java.math.BigDecimal.ZERO, "USD");
        }
        return Money.of(dto.amount(), dto.currency());
    }

    private MoneyDto toMoneyDto(Money money) {
        return new MoneyDto(money.getAmount(), money.getCurrencyCode());
    }
}
