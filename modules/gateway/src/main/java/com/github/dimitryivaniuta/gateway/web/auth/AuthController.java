package com.github.dimitryivaniuta.gateway.web.auth;

import com.github.dimitryivaniuta.gateway.config.properties.SecurityProperties;
import com.github.dimitryivaniuta.gateway.persistence.entity.UserEntity;
import com.github.dimitryivaniuta.gateway.persistence.service.JwtService;
import com.github.dimitryivaniuta.gateway.persistence.service.UserService;
import com.github.dimitryivaniuta.gateway.web.auth.dto.CurrentUserResponse;
import com.github.dimitryivaniuta.gateway.web.auth.dto.LoginRequest;
import com.github.dimitryivaniuta.gateway.web.auth.dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authentication REST API for the gateway.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Username/password login against USERS table and JWT token issuing.</li>
 *   <li>Expose current authenticated principal information.</li>
 * </ul>
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code POST /auth/token} – authenticate and issue JWT.</li>
 *   <li>{@code GET  /auth/me}    – return information about the current user.</li>
 * </ul>
 *
 * <p>Note: Access to {@code /auth/**} is configured as {@code permitAll} in
 * {@link com.github.dimitryivaniuta.gateway.config.SecurityConfig}, so login
 * works even when {@code security.require-auth=true}.</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          SecurityProperties securityProperties) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    /**
     * Authenticate user with tenant + username + password and issue a JWT access token.
     *
     * <p>Example request:
     * <pre>
     * POST /auth/token
     * {
     *   "tenantId": "default",
     *   "username": "admin",
     *   "password": "ChangeMe123!"
     * }
     * </pre>
     *
     * @return 200 OK with token response, or 401 if credentials are invalid.
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@RequestBody LoginRequest request) {
        Optional<UserEntity> userOpt = userService.authenticate(
                request.tenantId(), request.username(), request.password());

        if (userOpt.isEmpty()) {
            log.debug("Login failed for tenant='{}', username='{}'",
                    request.tenantId(), request.username());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UserEntity user = userOpt.get();
        String token = jwtService.issueToken(user);

        List<String> roles = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        long expiresIn = securityProperties.jwt().ttlSecondsOrDefault();

        TokenResponse response = new TokenResponse(
                token,
                "Bearer",
                expiresIn,
                user.getTenantId(),
                user.getUsername(),
                roles
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Return information about the currently authenticated principal.
     *
     * <p>Works for both JWT and API key authentication. For JWT, tenant/subject/scope
     * are taken from token claims. For API key, only principal and authorities are returned.</p>
     */
    @GetMapping("/me")
    public CurrentUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();

            String tenant = jwt.getClaimAsString("tenant");
            String username = jwt.getSubject();

            List<String> scopes = Optional.ofNullable(jwt.getClaimAsString("scope"))
                    .map(s -> List.of(s.split("\\s+")))
                    .orElseGet(List::of);

            List<String> authorities = jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            return new CurrentUserResponse(
                    "JWT",
                    tenant,
                    username,
                    scopes,
                    authorities
            );
        }

        // Fallback – e.g. API key authenticated principal
        String principal = String.valueOf(authentication.getPrincipal());
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new CurrentUserResponse(
                "OTHER",
                null,
                principal,
                List.of(),
                authorities
        );
    }

}
