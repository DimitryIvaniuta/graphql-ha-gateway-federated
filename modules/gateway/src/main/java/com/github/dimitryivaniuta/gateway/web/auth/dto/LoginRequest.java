package com.github.dimitryivaniuta.gateway.web.auth.dto;

/**
 * Login request payload for {@code POST /auth/token}.
 */
public record LoginRequest(
        String tenantId,
        String username,
        String password
) { }