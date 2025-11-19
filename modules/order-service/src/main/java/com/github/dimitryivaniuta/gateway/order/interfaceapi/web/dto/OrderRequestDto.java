package com.github.dimitryivaniuta.gateway.order.interfaceapi.web.dto;

/**
 * Placeholder for filter parameters for order searches.
 */
public record OrderRequestDto(
        String status,
        String customerExternalId
) {
}
