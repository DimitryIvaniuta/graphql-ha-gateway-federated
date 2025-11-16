package com.github.dimitryivaniuta.gateway.service;

import com.github.dimitryivaniuta.gateway.config.properties.SecurityProperties;
import com.github.dimitryivaniuta.gateway.persistence.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT service responsible for:
 * <ul>
 *   <li>Decoding/validating incoming JWTs (resource server behavior).</li>
 *   <li>Issuing new JWTs after a successful login (SSO token issuing).</li>
 * </ul>
 *
 * <p>Configuration (example):
 * <pre>
 * security:
 *   jwt:
 *     issuer: graphql-gateway
 *     ttl-seconds: 3600
 *     # secret is configured in SecurityConfig, see JwtEncoder/JwtDecoder beans
 * </pre>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtDecoder jwtDecoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;

    public JwtService(JwtDecoder jwtDecoder,
                      JwtEncoder jwtEncoder,
                      SecurityProperties securityProperties) {
        this.jwtDecoder = jwtDecoder;
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    /**
     * Decode a JWT string and wrap it into a {@link JwtAuthenticationToken}.
     *
     * @param token raw JWT string (without "Bearer " prefix)
     * @return authenticated token with authorities
     * @throws JwtException if decoding or validation fails
     */
    public JwtAuthenticationToken authenticate(String token) throws JwtException {
        Jwt jwt = jwtDecoder.decode(token);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());

        log.debug("Decoded JWT for subject='{}', authorities={}", jwt.getSubject(), authorities);
        return authentication;
    }

    /**
     * Issue a new JWT for a successfully authenticated user.
     *
     * <p>Payload contains:
     * <ul>
     *   <li>{@code sub} – username</li>
     *   <li>{@code tenant} – tenant identifier</li>
     *   <li>{@code scope} – space-delimited roles (ROLE_*)</li>
     * </ul>
     */
    public String issueToken(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(securityProperties.jwt().ttlSeconds());

        List<String> roleList = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        String scopeClaim = roleList.stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("tenant", user.getTenantId())
                .claim("scope", scopeClaim)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        JwtEncoderParameters params = JwtEncoderParameters.from(jwsHeader, claims);

        Jwt jwt = jwtEncoder.encode(params);
        log.debug("Issued JWT for tenant='{}', username='{}', expiresAt={}",
                user.getTenantId(), user.getUsername(), expiresAt);

        return jwt.getTokenValue();
    }

    /**
     * Extract Spring Security authorities from common JWT scope/role claims.
     *
     * <p>Strategy:
     * <ul>
     *   <li>Check {@code scope} claim (list or space-separated String).</li>
     *   <li>Also consider {@code scp} claim (Azure AD style list).</li>
     *   <li>Prefix each scope with {@code SCOPE_} to align with Spring's convention.</li>
     * </ul>
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> scopes = new ArrayList<>();

        // "scope" claim – list form
        List<String> scopeList = jwt.getClaimAsStringList("scope");
        if (scopeList != null) {
            scopes.addAll(scopeList);
        } else {
            // "scope" claim – space-delimited String form
            String scopeString = jwt.getClaimAsString("scope");
            if (scopeString != null && !scopeString.isBlank()) {
                scopes.addAll(List.of(scopeString.split("\\s+")));
            }
        }

        // "scp" claim – often used by Azure AD
        List<String> scpList = jwt.getClaimAsStringList("scp");
        if (scpList != null) {
            scopes.addAll(scpList);
        }

        return scopes.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .<GrantedAuthority>map(scope -> () -> "SCOPE_" + scope)
                .toList();
    }
}
