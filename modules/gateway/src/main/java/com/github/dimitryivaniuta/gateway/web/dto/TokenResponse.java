package com.github.dimitryivaniuta.gateway.web.dto;

import java.util.List;

/**
 * Token response returned after successful authentication.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String tenantId,
        String username,
        List<String> roles
) { }
