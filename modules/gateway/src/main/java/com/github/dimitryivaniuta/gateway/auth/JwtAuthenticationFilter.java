package com.github.dimitryivaniuta.gateway.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JWT-based authentication filter.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Inspect the {@code Authorization} header for a {@code Bearer &lt;token&gt;} value.</li>
 *   <li>Use a Spring {@link JwtDecoder} to validate and parse the token.</li>
 *   <li>On success, create a {@link JwtAuthenticationToken} and store it in the {@link SecurityContextHolder}.</li>
 * </ul>
 *
 * <p>Usage:
 * <ul>
 *   <li>Configure a {@link JwtDecoder} bean (via Spring Boot's resource-server support or custom config).</li>
 *   <li>Register this filter in {@code SecurityConfig}, e.g.:
 *   <pre>
 *   http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
 *   </pre>
 *   </li>
 * </ul>
 *
 * <p>Expected JWT configuration (example via properties):
 * <pre>
 * spring:
 *   security:
 *     oauth2:
 *       resourceserver:
 *         jwt:
 *           jwk-set-uri: https://example.com/.well-known/jwks.json
 * </pre>
 * Boot will auto-create a {@link JwtDecoder} which this filter can inject.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    /**
     * @param jwtDecoder Spring Security JWT decoder; typically auto-configured by Boot.
     */
    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Limit this filter to the GraphQL endpoint and only when an Authorization header is present.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (!"/graphql".equals(request.getServletPath())) {
            return true;
        }
        return authHeader == null || !authHeader.startsWith(BEARER_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            Jwt jwt = jwtDecoder.decode(token);

            // Map scopes/roles from JWT claims to Spring authorities.
            Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

            JwtAuthenticationToken authentication =
                    new JwtAuthenticationToken(jwt, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for subject '{}' on {} {}",
                    jwt.getSubject(), request.getMethod(), request.getRequestURI());

        } catch (JwtException ex) {
            log.warn("Invalid JWT presented on {} {}: {}", request.getMethod(), request.getRequestURI(),
                    ex.getMessage());
            // Reject request early with 401; no need to proceed further.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract authorities from standard JWT claims.
     *
     * <p>Strategy:
     * <ul>
     *   <li>Check {@code scope} or {@code scp} claims.</li>
     *   <li>Support both space-delimited String and String list forms.</li>
     *   <li>Prefix with {@code SCOPE_} to align with Spring's convention.</li>
     * </ul>
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> scopes = new ArrayList<>();

        // Try "scope" claim as list
        List<String> scopeList = jwt.getClaimAsStringList("scope");
        if (scopeList != null) {
            scopes.addAll(scopeList);
        } else {
            // Try "scope" as space-delimited String
            String scopeString = jwt.getClaimAsString("scope");
            if (scopeString != null && !scopeString.isBlank()) {
                scopes.addAll(List.of(scopeString.split("\\s+")));
            }
        }

        // Fallback to "scp" (Azure AD style)
        List<String> scpList = jwt.getClaimAsStringList("scp");
        if (scpList != null) {
            scopes.addAll(scpList);
        }

        // Map to Spring GrantedAuthority
        return scopes.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(scope -> (GrantedAuthority) () -> "SCOPE_" + scope)
                .toList();
    }
}
