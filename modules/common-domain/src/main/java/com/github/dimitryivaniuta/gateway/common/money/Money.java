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

    /**
     * Factory method.
     *
     * <ul>
     *   <li>{@code amount} must not be null</li>
     *   <li>{@code currencyCode} must not be null</li>
     *   <li>{@code currencyCode} must be a valid ISO-4217 code (e.g. "EUR")</li>
     *   <li>Amount is normalized to scale 2 using HALF_UP</li>
     * </ul>
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");

        if (amount.signum() == 0 || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        String normalizedCode = currencyCode.trim().toUpperCase();
        // Will throw IllegalArgumentException if code is invalid (e.g. "XYZ")
        Currency.getInstance(normalizedCode);

        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        return new Money(normalizedAmount, normalizedCode);
    }

    public static Money ofMinorUnits(long minorUnits, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        int scale = Math.max(currency.getDefaultFractionDigits(), 2);
        BigDecimal major = BigDecimal.valueOf(minorUnits, scale);
        return new Money(major, currency.getCurrencyCode());
    }

    /**
     * Adds another Money with the same currency.
     *
     * @throws IllegalArgumentException if currencies differ.
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currencyCode + " vs " + other.currencyCode);
        }
        return Money.of(this.amount.add(other.amount), this.currencyCode);
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
