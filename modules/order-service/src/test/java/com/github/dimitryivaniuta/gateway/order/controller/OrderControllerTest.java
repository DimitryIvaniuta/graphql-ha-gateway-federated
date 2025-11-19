package com.github.dimitryivaniuta.gateway.order.controller;
import com.github.dimitryivaniuta.gateway.common.money.Money;
import com.github.dimitryivaniuta.gateway.order.application.OrderApplicationService;
import com.github.dimitryivaniuta.gateway.order.domain.Order;
import com.github.dimitryivaniuta.gateway.order.domain.OrderId;
import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;
import com.github.dimitryivaniuta.gateway.order.interfaceapi.web.OrderController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC slice test for {@link OrderController}.
 *
 * Uses @MockitoBean OrderApplicationService to verify HTTP <-> application mapping.
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // <<< disable Spring Security filters for this test
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderApplicationService orderService;

    // -------------------------------------------------------------------------
    // /internal/orders?ids=...
    // -------------------------------------------------------------------------

    @Test
    void getByIds_returnsOrdersInRequestedOrder() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Order order1 = new Order(
                new OrderId(id1),
                UUID.randomUUID(),
                OrderStatus.CREATED,
                Money.of(new BigDecimal("10.00"), "EUR"),
                now,
                now,
                "ext-1"
        );

        Order order2 = new Order(
                new OrderId(id2),
                UUID.randomUUID(),
                OrderStatus.CONFIRMED,
                Money.of(new BigDecimal("20.00"), "EUR"),
                now,
                now,
                "ext-2"
        );

        // Return in reverse order to ensure controller reorders by ids param
        when(orderService.getOrders(List.of(id1, id2)))
                .thenReturn(List.of(order2, order1));

        mockMvc.perform(get("/internal/orders")
                        .param("ids", id1 + "," + id2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(id1.toString())))
                .andExpect(jsonPath("$[0].status", is("CREATED")))
                .andExpect(jsonPath("$[0].total.amount", is(10.00)))
                .andExpect(jsonPath("$[0].total.currency", is("EUR")))
                .andExpect(jsonPath("$[1].id", is(id2.toString())))
                .andExpect(jsonPath("$[1].status", is("CONFIRMED")))
                .andExpect(jsonPath("$[1].total.amount", is(20.00)))
                .andExpect(jsonPath("$[1].total.currency", is("EUR")));

        verify(orderService).getOrders(List.of(id1, id2));
    }

    // -------------------------------------------------------------------------
    // POST /internal/orders
    // -------------------------------------------------------------------------

    @Test
    void createInternal_createsOrderAndReturnsDto() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Order created = new Order(
                new OrderId(orderId),
                customerId,
                OrderStatus.CREATED,
                Money.of(new BigDecimal("30.00"), "USD"),
                now,
                now,
                "EXT-123"
        );

        when(orderService.createOrder(any())).thenReturn(created);

        String payload = """
            {
              "customerId": "%s",
              "externalId": "EXT-123",
              "items": [
                { "sku": "SKU-1", "quantity": 2, "price": { "amount": 10.00, "currency": "USD" } },
                { "sku": "SKU-2", "quantity": 1, "price": { "amount": 10.00, "currency": "USD" } }
              ]
            }
            """.formatted(customerId);

        mockMvc.perform(post("/internal/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.total.amount", is(30.0)))
                .andExpect(jsonPath("$.total.currency", is("USD")))
                .andExpect(jsonPath("$.externalId", is("EXT-123")));

        // Optional: capture and inspect command
        ArgumentCaptor<com.github.dimitryivaniuta.gateway.order.application.command.CreateOrderCommand> captor =
                ArgumentCaptor.forClass(com.github.dimitryivaniuta.gateway.order.application.command.CreateOrderCommand.class);
        verify(orderService).createOrder(captor.capture());
    }

    // -------------------------------------------------------------------------
    // PATCH /internal/orders/{id}/status
    // -------------------------------------------------------------------------

    @Test
    void changeStatusInternal_updatesStatusAndReturnsDto() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Order updated = new Order(
                new OrderId(orderId),
                customerId,
                OrderStatus.CONFIRMED,
                Money.of(new BigDecimal("50.00"), "USD"),
                now.minusDays(1),
                now,
                "EXT-999"
        );

        when(orderService.changeStatus(any())).thenReturn(updated);

        String payload = """
            { "status": "CONFIRMED" }
            """;

        mockMvc.perform(patch("/internal/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.toString())))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.total.amount", is(50.00)))
                .andExpect(jsonPath("$.total.currency", is("USD")));

        verify(orderService).changeStatus(any());
    }

    // -------------------------------------------------------------------------
    // GET /api/orders?page=&size=
    // -------------------------------------------------------------------------

    @Test
    void findPage_returnsPagedResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Order order = new Order(
                new OrderId(orderId),
                customerId,
                OrderStatus.CREATED,
                Money.of(new BigDecimal("12.34"), "EUR"),
                now,
                now,
                "EXT-555"
        );

        Page<Order> page = new PageImpl<>(
                List.of(order),
                PageRequest.of(0, 1),
                1
        );

        when(orderService.findPage(0, 1)).thenReturn(page);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(orderId.toString())))
                .andExpect(jsonPath("$.content[0].status", is("CREATED")))
                .andExpect(jsonPath("$.content[0].total.amount", is(12.34)))
                .andExpect(jsonPath("$.content[0].total.currency", is("EUR")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(orderService).findPage(0, 1);
    }
}
