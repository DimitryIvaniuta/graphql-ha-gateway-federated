package com.github.dimitryivaniuta.gateway.order.controller;

//import com.github.dimitryivaniuta.gateway.order.web.OrderController;
//import com.github.dimitryivaniuta.gateway.order.service.OrderService;
import com.github.dimitryivaniuta.gateway.order.application.OrderApplicationService;
import com.github.dimitryivaniuta.gateway.order.interfaceapi.web.OrderController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP contract tests for OrderController (slice test).
 *
 * Assumptions (aligns with common REST style in your stack):
 * - Base path: /api/orders
 * - GET /{id} -> 200 with JSON (id, status, total{amount,currency}, createdAt)
 * - POST /     -> 201 with Location header and body (created resource)
 * - PUT /{id}  -> 200 with updated body
 * - DELETE /{id} -> 204
 * - GET /?page=&size= -> 200 with page envelope {content, page, size, totalElements}
 *
 * Security filters are disabled here to focus on controller behavior.
 */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
final class OrderControllerTest {

    private static final String BASE = "/api/orders";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean
    OrderApplicationService orderService;

    // --- Happy path: GET by id ---
    @Test
    @DisplayName("GET /api/orders/{id} -> 200 with basic order payload")
    void getById_200() throws Exception {
        UUID id = UUID.randomUUID();
        // Mock service result; structure can be your Order DTO/map—controller will serialize it.
        Map<String, Object> dto = Map.of(
                "id", id.toString(),
                "status", "CREATED",
                "total", Map.of("amount", "123.45", "currency", "USD"),
                "createdAt", OffsetDateTime.parse("2025-01-01T10:15:30+00:00").toString()
        );
        Mockito.when(orderService.getById(eq(id))).thenReturn(dto);

        mvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.status", is("CREATED")))
                .andExpect(jsonPath("$.total.amount", is("123.45")))
                .andExpect(jsonPath("$.total.currency", is("USD")));
    }

    // --- Not found: GET by id ---
    @Test
    @DisplayName("GET /api/orders/{id} -> 404 when not found")
    void getById_404() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.when(orderService.getById(eq(id)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        mvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    // --- Create: POST ---
    @Test
    @DisplayName("POST /api/orders -> 201 with Location and body")
    void create_201() throws Exception {
        String body = """
            {
              "customerId": "c-001",
              "items": [
                {"sku": "SKU-1", "qty": 2, "price": {"amount":"10.00","currency":"USD"}},
                {"sku": "SKU-2", "qty": 1, "price": {"amount":"20.00","currency":"USD"}}
              ]
            }
            """;

        UUID id = UUID.randomUUID();
        Map<String, Object> created = Map.of(
                "id", id.toString(),
                "status", "CREATED",
                "total", Map.of("amount", "40.00", "currency", "USD")
        );

        Mockito.when(orderService.create(any())).thenReturn(created);

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(BASE + "/" + id)))
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.total.amount", is("40.00")))
                .andExpect(jsonPath("$.status", is("CREATED")));
    }

    // --- Bad request on validation ---
    @Test
    @DisplayName("POST /api/orders -> 400 on validation errors")
    void create_400() throws Exception {
        // Missing customerId / items invalid -> expect validation to fail if @Valid is on controller method
        String invalid = """
            { "customerId": "", "items": [] }
            """;

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    // --- Update: PUT ---
    @Test
    @DisplayName("PUT /api/orders/{id} -> 200 with updated resource")
    void update_200() throws Exception {
        UUID id = UUID.randomUUID();
        String patch = """
            { "status": "APPROVED" }
            """;

        Map<String, Object> updated = Map.of(
                "id", id.toString(),
                "status", "APPROVED",
                "total", Map.of("amount", "123.45", "currency", "USD")
        );

        Mockito.when(orderService.update(eq(id), any())).thenReturn(updated);

        mvc.perform(put(BASE + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    // --- Delete: DELETE ---
    @Test
    @DisplayName("DELETE /api/orders/{id} -> 204")
    void delete_204() throws Exception {
        UUID id = UUID.randomUUID();
        Mockito.doNothing().when(orderService).delete(eq(id));

        mvc.perform(delete(BASE + "/{id}", id))
                .andExpect(status().isNoContent());
    }

    // --- Page/List: GET ---
    @Test
    @DisplayName("GET /api/orders?page=&size= -> 200 with page envelope")
    void list_200() throws Exception {
        // Mock a simple page envelope your controller returns
        Map<String, Object> item = Map.of(
                "id", UUID.randomUUID().toString(),
                "status", "CREATED",
                "total", Map.of("amount", "12.34", "currency", "USD")
        );
        Map<String, Object> page = Map.of(
                "content", List.of(item),
                "page", 0,
                "size", 1,
                "totalElements", 1
        );

        Mockito.when(orderService.findPage(eq(0), eq(1), any()))
                .thenReturn(page);

        mvc.perform(get(BASE).param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    // --- Helpers (optional examples if you assert numeric totals) ---
    @Nested
    class _HelpersExample {
        @Test
        void bigDecimalExample_notPartOfControllerJustIllustrative() {
            // Illustrates money-style comparisons you might use in service/mapper unit tests.
            BigDecimal total = new BigDecimal("40.00");
            assert total.compareTo(new BigDecimal("40")) == 0;
        }
    }
}
