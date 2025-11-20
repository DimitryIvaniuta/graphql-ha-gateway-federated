package com.github.dimitryivaniuta.gateway.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtResourceServerProperties(
        String issuer,
        String secret
) { }