package com.github.dimitryivaniuta.gateway.graphql.type.input;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Java representation of the GraphQL CapturePaymentInput.
 */
public record CapturePaymentInput(
        UUID orderId,
        BigDecimal amount,
        String currency,
        String provider
) {
}
