package com.github.dimitryivaniuta.gateway.security;

package com.github.dimitryivaniuta.gateway.security;

import com.github.dimitryivaniuta.gateway.config.properties.SecurityProperties;
import com.github.dimitryivaniuta.gateway.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void issueToken_containsIssuerTenantSubjectAndScopes() {
        // given
        String issuer = "http://localhost:8080";
        String secret = "change-me-at-least-32-chars-long-secret";
        long ttlSeconds = 3600L;

        SecurityProperties.Jwt jwtProps = new SecurityProperties.Jwt(issuer, secret, ttlSeconds);
        SecurityProperties securityProps = new SecurityProperties(jwtProps, null, null);

        var keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        var key = new SecretKeySpec(keyBytes, "HmacSHA256");

        var jwtEncoder = JwtTestUtils.createHs256Encoder(key); // small helper below
        var jwtService = new JwtService(securityProps, jwtEncoder);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setTenantId("default");
        user.setUsername("admin");
        user.setRoles("ROLE_ADMIN,ROLE_USER");

        // when
        String token = jwtService.issueToken(user);

        // then: decode with same secret and verify claims
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getIssuer().toString()).isEqualTo(issuer);
        assertThat(decoded.getSubject()).isEqualTo("admin");
        assertThat(decoded.getClaimAsString("tenant")).isEqualTo("default");

        String scope = decoded.getClaimAsString("scope");
        assertThat(scope).isNotNull();

        var scopes = Set.of(scope.split("\\s+"));
        assertThat(scopes).contains("ADMIN", "USER", "orders.internal");

        Instant now = Instant.now();
        assertThat(decoded.getExpiresAt()).isAfter(now);
        assertThat(decoded.getIssuedAt()).isBefore(decoded.getExpiresAt());
    }
}
