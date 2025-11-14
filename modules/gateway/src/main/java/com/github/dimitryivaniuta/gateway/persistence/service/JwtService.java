package com.github.dimitryivaniuta.gateway.persistence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JWT-focused security service that wraps {@link JwtDecoder} and encapsulates
 * how authorities are extracted from JWT claims.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Decode and validate a JWT via {@link JwtDecoder}.</li>
 *   <li>Translate claims (e.g. {@code scope}, {@code scp}) into Spring Security authorities.</li>
 *   <li>Produce an authenticated {@link JwtAuthenticationToken} ready to be placed in the SecurityContext.</li>
 * </ul>
 *
 * <p>Configuration example (application.yml):
 * <pre>
 * spring:
 *   security:
 *     oauth2:
 *       resourceserver:
 *         jwt:
 *           jwk-set-uri: https://issuer.example.com/.well-known/jwks.json
 * </pre>
 * This lets Spring Boot auto-configure a {@link JwtDecoder} bean.</p>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtDecoder jwtDecoder;

    public JwtService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
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
