package com.github.dimitryivaniuta.gateway.order.infrastructure.persistence.entity;

import com.github.dimitryivaniuta.gateway.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MoneyEmbeddable {

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    public static MoneyEmbeddable fromMoney(Money money) {
        Objects.requireNonNull(money, "money must not be null");
        return MoneyEmbeddable.builder()
                .amount(money.getAmount())
                .currency(money.getCurrencyCode())
                .build();
    }

    public Money toMoney() {
        return Money.of(amount, currency);
    }

}