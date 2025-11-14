package com.github.dimitryivaniuta.gateway.auth;

import com.github.dimitryivaniuta.gateway.persistence.entity.ApiKeyEntity;
import com.github.dimitryivaniuta.gateway.persistence.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * A Filter that checks for a valid API-Key header in incoming HTTP requests, authenticates the request
 * if valid, or rejects it otherwise.
 *
 * <p>This filter should be registered early in the filter chain (before JwtAuthenticationFilter or UsernamePasswordAuthenticationFilter)
 * and only applied when API-key access is allowed. It supports stateless authentication of clients identified via API keys stored in the DB.</p>
 *
 * <p>Workflow:
 * <ol>
 *   <li>Extract header (default “X-API-Key” or configurable).</li>
 *   <li>If present, validate via {@link ApiKeyService} (enabled, not expired, rate limit etc.).</li>
 *   <li>If valid, build an {@link Authentication} and set it in the {@code SecurityContext}.</li>
 *   <li>If invalid, respond HTTP 401 Unauthorized and stop filter chain.</li>
 *   <li>If header missing, simply move on—possibly other auth (JWT) may apply.</li>
 * </ol>
 * </p>
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-API-Key";

    private final ApiKeyService apiKeyService;

    /**
     * Constructor.
     *
     * @param apiKeyService service to validate API keys.
     */
    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            // No API key present -> skip this filter and allow other authentication mechanisms (e.g., JWT) to proceed
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ApiKeyEntity keyEntity = apiKeyService.validateAndFetch(apiKey);
            if (!keyEntity.isEnabled()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key disabled");
                return;
            }
            // Build authorities (e.g., role from entity)
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(keyEntity.getRole())
            );
            Authentication authentication = new ApiKeyAuthenticationToken(
                    keyEntity.getKey(),
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            // Any validation failure results in 401
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired API key");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Optionally skip API-key logic for certain paths (for example actuator or static)
        String path = request.getServletPath();
        return path.startsWith("/actuator") || path.startsWith("/graphiql");
    }

    /**
     * Inner static authentication token representing API key credentials.
     */
    private static class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
        private final String principal;

        public ApiKeyAuthenticationToken(String principal, List<SimpleGrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return principal;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
