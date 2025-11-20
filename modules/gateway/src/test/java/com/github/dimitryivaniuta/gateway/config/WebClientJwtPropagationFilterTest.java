package com.github.dimitryivaniuta.gateway.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientJwtPropagationFilterTest {

    private final WebClientConfig config = new WebClientConfig();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtPropagationFilter_addsBearerHeader_whenJwtPresent() {
        // given
        Jwt jwt = new Jwt(
                "dummy-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "admin")
        );
        var auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        var filter = config.jwtPropagationFilter();

        ClientRequest request = ClientRequest.create()
                .url(URI.create("http://order-service/internal/orders"))
                .build();

        final String[] authHeader = new String[1];

        ExchangeFunction next = clientRequest -> {
            authHeader[0] = clientRequest.headers().getFirst("Authorization");
            return Mono.empty();
        };

        // when
        filter.filter(request, next).block();

        // then
        assertThat(authHeader[0]).isEqualTo("Bearer dummy-token-value");
    }

    @Test
    void jwtPropagationFilter_doesNotAddHeader_whenNoAuthentication() {
        var filter = config.jwtPropagationFilter();

        ClientRequest request = ClientRequest.create()
                .url(URI.create("http://order-service/internal/orders"))
                .build();

        final String[] authHeader = new String[1];

        ExchangeFunction next = clientRequest -> {
            authHeader[0] = clientRequest.headers().getFirst("Authorization");
            return Mono.empty();
        };

        // when
        filter.filter(request, next).block();

        // then
        assertThat(authHeader[0]).isNull();
    }
}
