package com.github.dimitryivaniuta.gateway.auth;

import com.github.dimitryivaniuta.gateway.persistence.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authentication filter for JWT-based Single Sign-On.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Inspect the {@code Authorization} header for a {@code Bearer &lt;token&gt;} value.</li>
 *   <li>Delegate JWT decoding and validation to {@link JwtService}.</li>
 *   <li>On success, populate the {@link SecurityContextHolder} with a {@link JwtAuthenticationToken}.</li>
 *   <li>On failure, respond with HTTP 401 and do not continue the filter chain.</li>
 * </ul>
 *
 * <p>Filter activation:
 * <ul>
 *   <li>Runs only if the request contains an {@code Authorization} header
 *       starting with {@code "Bearer "}.</li>
 *   <li>If the header is absent or not a Bearer token, this filter is skipped
 *       and the request proceeds as anonymous (subject to security rules).</li>
 * </ul>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /**
     * @param jwtService service that handles decoding, validation and converting
     *                   JWTs into {@link JwtAuthenticationToken}.
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Skip this filter if there is no Bearer token.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        return authHeader == null || !authHeader.startsWith(BEARER_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        // Do not override an existing authenticated context (e.g. API key already set it).
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Delegate decoding + authority extraction to JwtService
            JwtAuthenticationToken authentication = jwtService.authenticate(token);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for subject='{}' on {} {}",
                    authentication.getName(), request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);

        } catch (JwtException ex) {
            // Invalid or expired token → reject with 401
            log.warn("Invalid JWT on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
