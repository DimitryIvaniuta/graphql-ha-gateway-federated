package com.github.dimitryivaniuta.gateway.config;

import com.github.dimitryivaniuta.gateway.auth.ApiKeyAuthenticationFilter;
import com.github.dimitryivaniuta.gateway.auth.JwtAuthenticationFilter;
import com.github.dimitryivaniuta.gateway.config.properties.CorsProperties;
import com.github.dimitryivaniuta.gateway.config.properties.SecurityProperties;
import com.github.dimitryivaniuta.gateway.persistence.service.ApiKeyService;
import com.github.dimitryivaniuta.gateway.persistence.service.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Central Spring Security configuration for the gateway.
 *
 * <p>Key characteristics:
 * <ul>
 *   <li>Stateless (no HTTP session for security).</li>
 *   <li>Property-driven toggle {@code security.require-auth} to switch between open dev
 *       and locked-down mode.</li>
 *   <li>Pluggable authentication mechanisms:
 *       <ul>
 *         <li>JWT tokens via {@link JwtService} and {@link JwtAuthenticationFilter}.</li>
 *         <li>API keys via {@link ApiKeyService} and {@link ApiKeyAuthenticationFilter}.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>High-level behavior:
 * <ul>
 *   <li>Always permits {@code /actuator/health/**} and {@code /actuator/info}.</li>
 *   <li>Always permits GraphiQL resources ({@code /graphiql/**}).</li>
 *   <li>When {@code security.require-auth=false} (default):
 *       <ul>
 *         <li>{@code /graphql} &gt; various APIs – all permitted (dev mode).</li>
 *       </ul>
 *   </li>
 *   <li>When {@code security.require-auth=true}:
 *       <ul>
 *         <li>/graphql and all other non-actuator paths require authentication
 *             (JWT and/or API key).</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({SecurityProperties.class, CorsProperties.class})
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    private final CorsProperties corsProperties;

    /**
     * Main HTTP security configuration and filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiKeyAuthenticationFilter apiKeyFilter,
                                                   JwtAuthenticationFilter jwtFilter) throws Exception {

        // Stateless API – no HTTP session state
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // No CSRF for stateless APIs
        http.csrf(csrf -> csrf.disable());

        // CORS setup via corsConfigurationSource bean
        http.cors(Customizer.withDefaults());

        // No form login or HTTP Basic – we rely on API keys / JWTs.
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        // Authorization rules
        http.authorizeHttpRequests(registry -> {
            // Allow health + info for infra probes.
            registry
                    .requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll();

            // Developer tooling (GraphiQL). In production you may want to restrict this further.
            registry
                    .requestMatchers("/graphiql", "/graphiql/**", "/vendor/graphiql/**")
                    .permitAll();

            if (securityProperties.requireAuth()) {
                // Lockdown mode – all non-actuator endpoints require authentication.
                registry
                        .requestMatchers(HttpMethod.POST, "/graphql").authenticated()
                        .requestMatchers(HttpMethod.GET, "/graphql").denyAll()
                        .anyRequest().authenticated();
            } else {
                // Dev mode – allow everything except GET /graphql.
                registry
                        .requestMatchers("/graphql").permitAll()
                        .anyRequest().permitAll();
            }
        });

        // Plug filters into the chain:
        // 1) JWT from Authorization: Bearer <token>
        // 2) API key from X-API-Key (or configured header)
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration used by the SecurityFilterChain.
     *
     * <p>By default, allows all origins (dev-friendly). In production override
     * {@code cors.allowed-origins} with a comma-separated allow list.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(corsProperties.resolvedAllowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-Key"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3_600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Symmetric signing key for JWTs (HS256) - JWT encoder (HS256) using security.jwt.secret..
     *
     * <p>Use a long, random secret in production and store it securely (e.g. Vault, Secrets Manager).</p>
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey key = new SecretKeySpec(securityProperties.jwt().secretRequired()
                .getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * Symmetric JWT decoder (HS256) using the same secret as the encoder.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(securityProperties.jwt().secretRequired()
                .getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * BCrypt encoder for user passwords (USERS table).
     * Password encoder placeholder for future username/password flows.
     * Not used by API-key/JWT flows directly.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * API-key authentication filter that delegates validation to {@link ApiKeyService}.
     *
     * @param apiKeyService  service used to look up active API keys
     */
    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(ApiKeyService apiKeyService) {

        return new ApiKeyAuthenticationFilter(apiKeyService, securityProperties);
    }

    /**
     * JWT authentication filter that delegates decoding and authority extraction
     * to {@link JwtService}.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }


}
