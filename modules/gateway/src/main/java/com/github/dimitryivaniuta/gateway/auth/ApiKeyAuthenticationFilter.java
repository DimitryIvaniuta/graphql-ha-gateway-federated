package com.github.dimitryivaniuta.gateway.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Simple API-key based authentication filter.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Look for an API key in the configured header (defaults to {@code X-API-Key}).</li>
 *   <li>Compare the incoming key to a static expected value from configuration.</li>
 *   <li>If the key matches and there is no existing authentication, create an authenticated
 *       {@link UsernamePasswordAuthenticationToken} with {@code ROLE_API_CLIENT}.</li>
 * </ul>
 *
 * <p>Configuration properties:
 * <pre>
 * security:
 *   api-key-header: X-API-Key        # optional, default
 *   static-api-key: my-super-secret  # optional; if empty, filter is effectively disabled
 * </pre>
 *
 * <p>Notes:
 * <ul>
 *   <li>This is intentionally simple – a real system would validate against a database
 *       or {@code ApiKeyService} to check status, rate limits, tenant, etc.</li>
 *   <li>The filter only runs for {@code /graphql} requests (see {@link #shouldNotFilter}).</li>
 * </ul>
 *
 * <p>To plug it into the chain, register it in {@code SecurityConfig} with:
 * <pre>
 * http.addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
 * </pre>
 * </p>
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final String headerName;
    private final String expectedApiKey;
    private final boolean enabled;

    /**
     * @param headerName    HTTP header name to read the API key from (e.g. X-API-Key).
     * @param expectedApiKey static API key configured for this gateway. If blank, filter is disabled.
     */
    public ApiKeyAuthenticationFilter(
            @Value("${security.api-key-header:X-API-Key}") String headerName,
            @Value("${security.static-api-key:}") String expectedApiKey) {

        this.headerName = Objects.requireNonNull(headerName, "headerName must not be null");
        this.expectedApiKey = expectedApiKey == null ? "" : expectedApiKey.trim();
        this.enabled = !this.expectedApiKey.isEmpty();
    }

    /**
     * Limit this filter to the GraphQL endpoint only (POST /graphql).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/graphql".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // If no static API key is configured, do nothing – filter behaves as a no-op.
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerValue = request.getHeader(headerName);

        if (headerValue == null || headerValue.isBlank()) {
            // No API key present – just continue. Security rules will decide whether
            // anonymous access is allowed for this route.
            filterChain.doFilter(request, response);
            return;
        }

        // Compare with expected key. If mismatch, short-circuit with 401.
        if (!expectedApiKey.equals(headerValue.trim())) {
            log.warn("Invalid API key from {} on {} {}", request.getRemoteAddr(),
                    request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // If the SecurityContext already has an authentication, do not override it.
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing == null || !existing.isAuthenticated()) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            "api-key-client", // principal
                            null,             // credentials
                            List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
                    );

            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("Authenticated request with API key for {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
