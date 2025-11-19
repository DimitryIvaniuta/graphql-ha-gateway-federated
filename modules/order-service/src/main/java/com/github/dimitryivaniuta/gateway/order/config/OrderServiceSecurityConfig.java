package com.github.dimitryivaniuta.gateway.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * order-service is a JWT-protected resource server.
 *
 * - /internal/** requires a JWT and "SCOPE_orders.internal" authority
 *   (coming from scope "orders.internal" in the token).
 * - /actuator/health, /actuator/info are public.
 * - all other endpoints require authentication.
 */
@Configuration
@EnableMethodSecurity
public class OrderServiceSecurityConfig {

    private static final String INTERNAL_SCOPE = "orders.internal";

    @Bean
    public SecurityFilterChain orderSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/internal/**").hasAuthority("SCOPE_" + INTERNAL_SCOPE)
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * Map JWT "scope" or "scp"/"scopes" claims to Spring authorities "SCOPE_xxx".
     */
    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = extractScopeAuthorities(jwt);
            String principalName = jwt.getSubject();
            return new JwtAuthenticationToken(jwt, authorities, principalName);
        };
    }

    private Collection<GrantedAuthority> extractScopeAuthorities(Jwt jwt) {
        Set<String> scopes = new HashSet<>();

        // Standard "scope" claim (space-separated string)
        Optional.ofNullable(jwt.getClaimAsString("scope"))
                .ifPresent(scopeStr -> scopes.addAll(Arrays.asList(scopeStr.split(" "))));

        // Sometimes "scp" or "scopes" is used as array
        Optional.ofNullable(jwt.getClaimAsStringList("scp"))
                .ifPresent(scopes::addAll);

        Optional.ofNullable(jwt.getClaimAsStringList("scopes"))
                .ifPresent(scopes::addAll);

        return scopes.stream()
                .map(scope -> "SCOPE_" + scope)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
