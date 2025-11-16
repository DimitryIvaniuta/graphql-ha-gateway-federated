package com.github.dimitryivaniuta.gateway.test;

import com.github.dimitryivaniuta.gateway.common.money.Money;
import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;
import com.github.dimitryivaniuta.gateway.graphql.type.Payment;
import com.github.dimitryivaniuta.gateway.graphql.type.input.CapturePaymentInput;
import com.github.dimitryivaniuta.gateway.persistence.service.client.InventoryClient;
import com.github.dimitryivaniuta.gateway.persistence.service.client.OrderClient;
import com.github.dimitryivaniuta.gateway.persistence.service.client.PaymentClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight contract tests for REST clients (OrderClient, InventoryClient, PaymentClient).
 *
 * <p>Verifies URLs and basic mapping using MockWebServer.</p>
 */
class RestClientContractTest {

    private static MockWebServer mockServer;

    @BeforeAll
    static void startServer() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    static void shutdownServer() throws Exception {
        mockServer.shutdown();
    }

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();
    }

    @Test
    void orderClient_getOrdersByIds_sendsCorrectQueryParam() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")  // empty JSON array is fine for flux mapping
                .addHeader("Content-Type", "application/json"));

        OrderClient client = new OrderClient(webClient());

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        client.getOrdersByIds(ids);

        var request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).startsWith("/internal/orders?ids=");
    }

    @Test
    void inventoryClient_getInventoryItemsByOrderIds_sendsCorrectQueryParam() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        InventoryClient client = new InventoryClient(webClient());

        var orderIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        client.getInventoryItemsByOrderIds(Set.copyOf(orderIds));

        var request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).startsWith("/internal/inventory/by-orders?orderIds=");
    }

    @Test
    void paymentClient_capturePayment_postsToCorrectEndpoint() throws Exception {
        // minimal valid Payment JSON – include all record components
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String body = """
                {
                  "id": "%s",
                  "orderId": "%s",
                  "status": "CAPTURED",
                  "amount": 100.00,
                  "currency": "EUR",
                  "provider": "stripe",
                  "createdAt": "2025-01-01T10:00:00Z",
                  "updatedAt": "2025-01-01T10:00:00Z"
                }
                """.formatted(paymentId, orderId);

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        PaymentClient client = new PaymentClient(webClient());

        CapturePaymentInput input = new CapturePaymentInput(
                orderId,
                MoneyDto.from(
                        Money.of(
                                BigDecimal.valueOf(100.00),
                                "EUR"
                        )
                ),
                "stripe"
        );

        Payment payment = client.capturePayment(input);

        var request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/internal/payments/capture");
        assertThat(payment.id()).isEqualTo(paymentId);
        assertThat(payment.orderId()).isEqualTo(orderId);
    }
}
