package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for changing order status.
 */
public record ChangeOrderStatusRequestDto(
        @NotNull OrderStatus status
) {
}
