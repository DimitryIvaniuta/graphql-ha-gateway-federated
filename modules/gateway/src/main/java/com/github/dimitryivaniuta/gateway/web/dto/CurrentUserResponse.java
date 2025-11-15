package com.github.dimitryivaniuta.gateway.web.dto;

import java.util.List;

/**
 * View of the current authenticated principal.
 */
public record CurrentUserResponse(
        String authType,
        String tenantId,
        String username,
        List<String> scopes,
        List<String> authorities
) { }