package com.github.dimitryivaniuta.gateway.order.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Simple value object for money (amount + currency).
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }
}
