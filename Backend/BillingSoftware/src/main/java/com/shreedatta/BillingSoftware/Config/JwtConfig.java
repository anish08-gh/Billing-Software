package com.shreedatta.BillingSoftware.Config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Bean
    public JwtDecoder jwtDecoder() {

        validateSecret();

        SecretKeySpec secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(
                        org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256
                )
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {

        validateSecret();

        SecretKeySpec secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );
    }

    private void validateSecret() {

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT_SECRET environment variable is required"
            );
        }

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET must be at least 32 bytes long"
            );
        }
    }
}