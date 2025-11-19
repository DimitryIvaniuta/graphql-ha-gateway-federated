package com.github.dimitryivaniuta.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.order")
public record OrderServiceProperties(String baseUrl) { }