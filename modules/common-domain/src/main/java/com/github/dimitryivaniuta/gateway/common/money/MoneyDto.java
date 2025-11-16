package com.github.dimitryivaniuta.gateway.common.money;

import java.math.BigDecimal;

/**
 * Transport representation of Money (JSON / GraphQL).
 */
public record MoneyDto(BigDecimal amount, String currency) {

    public static MoneyDto from(Money money) {
        return new MoneyDto(money.getAmount(), money.getCurrencyCode());
    }

    public Money toValueObject() {
        return Money.of(amount, currency);
    }
}
