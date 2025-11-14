package com.github.dimitryivaniuta.gateway.config;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Baseline Spring Security configuration for the GraphQL gateway.
 *
 * <p>Goals:
 * <ul>
 *     <li>Stateless API (no HTTP session state).</li>
 *     <li>Secure-by-default; health/info and GraphiQL are explicitly allowed.</li>
 *     <li>Feature flag {@code security.require-auth} to tighten access later without
 *         touching controllers/resolvers.</li>
 * </ul>
 *
 * <p>Behavior:
 * <ul>
 *   <li>{@code security.require-auth=false} (default):
 *       <ul>
 *         <li>{@code /actuator/health/**}, {@code /actuator/info} – allowed.</li>
 *         <li>{@code /graphiql/**} – allowed (developer tooling).</li>
 *         <li>{@code /graphql} – allowed (anonymous access).</li>
 *         <li>Any other path – allowed.</li>
 *       </ul>
 *   </li>
 *   <li>{@code security.require-auth=true}:
 *       <ul>
 *         <li>{@code /actuator/health/**}, {@code /actuator/info} – still allowed.</li>
 *         <li>Everything else – requires authentication (once an auth mechanism is wired).</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>Note: Authentication mechanism (API key, JWT, OAuth2, etc.) is intentionally not implemented yet.
 * Turning {@code security.require-auth=true} without an auth filter will effectively block non-actuator traffic.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Feature flag that allows you to flip from "open dev" to "locked down" without code changes.
     */
    @Value("${security.require-auth:false}")
    private boolean requireAuth;

    /**
     * Main HTTP security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Stateless API; security context is not stored in HTTP sessions.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // CSRF is unnecessary for stateless APIs (no browser form logins).
        http.csrf(csrf -> csrf.disable());

        // Basic CORS configuration; detailed rules via corsConfigurationSource().
        http.cors(Customizer.withDefaults());

        // We do not use form login or HTTP basic for now – this is an API gateway.
        http.formLogin(form -> form.disable());
        http.httpBasic(basic -> basic.disable());

        http.authorizeHttpRequests(registry -> {
                    // Ops endpoints are always open so Kubernetes / infra can probe health.
                    registry
                            .requestMatchers("/actuator/health/**", "/actuator/info")
                            .permitAll();

                    // Developer tooling – keep open in local/dev; consider restricting in production.
                    registry
                            .requestMatchers("/graphiql", "/graphiql/**", "/vendor/graphiql/**")
                            .permitAll();

                    // GraphQL endpoint + everything else based on the feature flag.
                    if (requireAuth) {
                        // When you plug in JWT/API-key/etc., these will really require authentication.
                        registry
                                .requestMatchers(HttpMethod.POST, "/graphql").authenticated()
                                .requestMatchers(HttpMethod.GET, "/graphql").denyAll() // no GET /graphql
                                .anyRequest().authenticated();
                    } else {
                        // Development / local mode: all endpoints (except GET /graphql) are open.
                        registry
                                .requestMatchers("/graphql").permitAll()
                                .anyRequest().permitAll();
                    }
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        // HSTS/HTTPS enforcement can be configured at ingress level; leave to infra for now.
        return http.build();
    }

    /**
     * CORS policy for browser clients (e.g. GraphiQL in SPA or API explorer).
     *
     * <p>By default this allows any origin. For production, override {@code cors.allowed-origins}
     * with a comma-separated allow-list, e.g. {@code https://app.example.com,https://admin.example.com}.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:*}") String allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated list; "*" means "any origin".
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // For APIs without credentials, AllowedOrigins="*" is acceptable.
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-Key"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3_600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Password encoder stub, ready for future user/credential-based flows if needed.
     * Not used yet, but required once you introduce any password-based authentication.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
