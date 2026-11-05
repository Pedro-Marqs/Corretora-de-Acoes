package com.projeto.gestao.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtSecurityProperties(
        boolean enabled,
        boolean sessionCompatibility,
        String issuer,
        String audience,
        Duration accessTokenTtl,
        String secret) {

    public JwtSecurityProperties {
        issuer = issuer == null || issuer.isBlank() ? "corretora-api" : issuer;
        audience = audience == null || audience.isBlank() ? "corretora-investors" : audience;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(10) : accessTokenTtl;
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("JWT access token TTL must be positive");
        }
    }
}
