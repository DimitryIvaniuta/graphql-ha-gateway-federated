package com.github.dimitryivaniuta.gateway.auth;

import com.github.dimitryivaniuta.gateway.config.properties.SecurityProperties;
import com.github.dimitryivaniuta.gateway.persistence.entity.ApiKeyEntity;
import com.github.dimitryivaniuta.gateway.persistence.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final String headerName;
    private final String staticKeyOrNull;

    /**
     * Constructor.
     *
     * @param apiKeyService service to validate API keys.
     */
    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService,
                                      SecurityProperties securityProperties) {
        this.apiKeyService = apiKeyService;
        SecurityProperties.ApiKey apiProps = securityProperties.apiKey();
        this.headerName = apiProps != null ? apiProps.headerOrDefault() : "X-API-Key";
        this.staticKeyOrNull = apiProps != null ? apiProps.staticKeyOrNull() : null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String headerValue = request.getHeader(headerName);

        if (headerValue == null || headerValue.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = headerValue.trim();

        // 1) DB-backed API key
        Optional<ApiKeyEntity> apiKeyOpt = apiKeyService.authenticate(token);

        if (apiKeyOpt.isEmpty()) {
            // 2) Optional static key fallback
            if (staticKeyOrNull != null && staticKeyOrNull.equals(token)) {
                log.debug("Authenticated using static API key for {}", request.getRequestURI());
                authenticateAs("static-api-key");
                filterChain.doFilter(request, response);
                return;
            }

            log.warn("Invalid API key from {} on {} {}", request.getRemoteAddr(),
                    request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        ApiKeyEntity apiKey = apiKeyOpt.get();
        authenticateAs("api-key:" + apiKey.getId());

        filterChain.doFilter(request, response);
    }

    private void authenticateAs(String principal) {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()) {
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
                );

        // details set in filter; WebAuthenticationDetailsSource requires HttpServletRequest,
        // so we leave it out here and set only principal/authorities.
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Optionally skip API-key logic for certain paths (for example actuator or static)
        return !"/graphql".equals(request.getServletPath());
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
