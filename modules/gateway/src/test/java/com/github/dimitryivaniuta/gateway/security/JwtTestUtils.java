package com.github.dimitryivaniuta.gateway.security;

import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;

final class JwtTestUtils {

    private JwtTestUtils() {
    }

    static JwtEncoder createHs256Encoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).build();
    }
}
