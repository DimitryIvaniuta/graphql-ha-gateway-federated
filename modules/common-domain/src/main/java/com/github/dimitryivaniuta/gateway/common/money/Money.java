package com.github.dimitryivaniuta.gateway.common.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable value object representing a monetary amount + ISO 4217 currency.
 *
 * - Always use BigDecimal for exact arithmetic.
 * - Scale is normalized to 2 fraction digits by default (can be adjusted).
 * - Designed to be used in JPA entities via @Embedded.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Money {

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currencyCode;

    /**
     * JPA requires a no-arg constructor.
     */
    @SuppressWarnings("unused")
    protected Money() {
        // for JPA
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
        Currency currency = Currency.getInstance(currencyCode);
        int scale = Math.max(currency.getDefaultFractionDigits(), 2);
        return new Money(amount.setScale(scale, RoundingMode.HALF_UP), currency.getCurrencyCode());
    }

    public static Money ofMinorUnits(long minorUnits, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        int scale = Math.max(currency.getDefaultFractionDigits(), 2);
        BigDecimal major = BigDecimal.valueOf(minorUnits, scale);
        return new Money(major, currency.getCurrencyCode());
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }

    public Money multiply(long factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currencyCode);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currencyCode);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(this.currencyCode, other.currencyCode)
            );
        }
    }
}
