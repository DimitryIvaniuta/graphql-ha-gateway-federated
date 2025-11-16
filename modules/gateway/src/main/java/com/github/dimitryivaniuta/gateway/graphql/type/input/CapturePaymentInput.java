package com.github.dimitryivaniuta.gateway.graphql.type.input;

import com.github.dimitryivaniuta.gateway.common.money.MoneyDto;

import java.util.UUID;

/**
 * Java representation of the GraphQL CapturePaymentInput.
 */
public record CapturePaymentInput(
        UUID orderId,
        MoneyDto total,
        String provider
) {
}
