package com.github.dimitryivaniuta.gateway.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void createsMoneyWithValidValues() {
        Money money = Money.of(new BigDecimal("10.50"), "EUR");

        assertThat(money.getAmount()).isEqualByComparingTo("10.50");
        assertThat(money.getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void negativeAmountIsNotAllowed() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of(new BigDecimal("-0.01"), "EUR")
        );
        assertThat(ex.getMessage()).containsIgnoringCase("amount must be positive");
    }

    @Test
    void nullAmountIsNotAllowed() {
        assertThrows(NullPointerException.class, () -> Money.of(null, "EUR"));
    }

    @Test
    void nullCurrencyIsNotAllowed() {
        assertThrows(NullPointerException.class, () -> Money.of(BigDecimal.ZERO, null));
    }

    @Test
    void blankCurrencyIsNotAllowed() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Money.of(BigDecimal.ZERO, "  ")
        );
        assertThat(ex.getMessage()).containsIgnoringCase("amount must be positive");
    }

    @Test
    void equalityAndHashCodeAreBasedOnAmountAndCurrency() {
        Money a = Money.of(new BigDecimal("10.00"), "USD");
        Money b = Money.of(new BigDecimal("10.00"), "USD");
        Money c = Money.of(new BigDecimal("20.00"), "USD");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }
}
