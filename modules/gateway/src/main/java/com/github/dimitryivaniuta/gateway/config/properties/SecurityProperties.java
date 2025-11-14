package com.github.dimitryivaniuta.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration for security concerns of the gateway.
 *
 * <p>Prefix: {@code security}</p>
 *
 * <pre>
 * security:
 *   require-auth: false
 *   api-key:
 *     header: X-API-Key
 *     static-key: ${GATEWAY_STATIC_API_KEY:}
 *   jwt:
 *     issuer: graphql-gateway
 *     ttl-seconds: 3600
 *     secret: ${JWT_SECRET:change-this-secret}
 * </pre>
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
        boolean requireAuth,
        ApiKey apiKey,
        Jwt jwt
) {

    /**
     * API-key related configuration.
     */
    public record ApiKey(
            String header,
            String staticKey
    ) {
        public String headerOrDefault() {
            return header != null && !header.isBlank() ? header : "X-API-Key";
        }

        public String staticKeyOrNull() {
            return (staticKey != null && !staticKey.isBlank()) ? staticKey.trim() : null;
        }
    }

    /**
     * JWT-related configuration: issuer, TTL, signing secret.
     */
    public record Jwt(
            String issuer,
            long ttlSeconds,
            String secret
    ) {
        public String issuerOrDefault() {
            return issuer != null && !issuer.isBlank() ? issuer : "graphql-gateway";
        }

        public long ttlSecondsOrDefault() {
            return ttlSeconds > 0 ? ttlSeconds : 3600L;
        }

        public String secretRequired() {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("security.jwt.secret must be configured");
            }
            return secret;
        }
    }
}
