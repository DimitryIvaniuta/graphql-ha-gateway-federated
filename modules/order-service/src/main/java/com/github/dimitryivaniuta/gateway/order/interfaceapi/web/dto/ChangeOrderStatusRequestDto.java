package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import com.github.dimitryivaniuta.gateway.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOrderStatusRequestDto(
        @NotNull OrderStatus status
) { }
