package com.github.dimitryivaniuta.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS configuration for the gateway.
 *
 * <p>Prefix: {@code cors}</p>
 *
 * <pre>
 * cors:
 *   allowed-origins: ${CORS_ALLOWED_ORIGINS:*}
 * </pre>
 *
 * <p>Environment examples:
 * <pre>
 * CORS_ALLOWED_ORIGINS=http://localhost:3000,https://app.example.com
 * </pre>
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    /**
     * Returns configured origins or a wildcard if none provided.
     */
    public List<String> resolvedAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return List.of("*");
        }
        return allowedOrigins;
    }
}
