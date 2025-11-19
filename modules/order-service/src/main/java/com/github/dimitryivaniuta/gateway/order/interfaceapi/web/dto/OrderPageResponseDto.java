package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

import lombok.Builder;

import java.util.List;

/**
 * Simple page wrapper for order listings.
 */
@Builder
public record OrderPageResponseDto(
        List<OrderResponseDto> content,
        int page,
        int size,
        long totalElements
) {
}
