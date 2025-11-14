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
 * A Filter to parse and validate JWT tokens in incoming HTTP requests.
 * If a valid token is present, authenticates the user and populates the SecurityContext.
 * If invalid, returns HTTP 401 and halts processing.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Extract token (e.g., “Authorization: Bearer {token}”).</li>
 *   <li>Use {@link JwtService} to validate, parse principal and claims.</li>
 *   <li>If valid, build an {@link Authentication}, set context and continue chain.</li>
 *   <li>If invalid, respond HTTP 401 Unauthorized.</li>
 *   <li>If no token present, skip – other auth (API key) may apply.</li>
 * </ol>
 * </p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Constructor.
     *
     * @param jwtService service to validate and extract claims from JWT.
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // No JWT – allow other auth mechanisms
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
            // Example: get username and roles from claims
            String username = claims.username();
            List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            Authentication authentication = new JwtAuthenticationToken(username, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (JwtAuthenticationException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip for actuator/health, static resources or other open endpoints
        String path = request.getServletPath();
        return path.startsWith("/actuator") || path.startsWith("/graphiql");
    }

    /**
     * Inner static authentication token for JWT‐authenticated users.
     */
    private static class JwtAuthenticationToken extends AbstractAuthenticationToken {
        private final String principal;

        public JwtAuthenticationToken(String principal, List<SimpleGrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
