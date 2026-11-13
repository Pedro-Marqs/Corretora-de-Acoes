package com.projeto.gestao.config;

import java.time.Duration;

import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.admin-jwt")
public record AdminJwtSecurityProperties(
        boolean enabled,
        String issuer,
        String audience,
        Duration accessTokenTtl,
        String secret,
        Duration refreshTokenTtl) {

    public AdminJwtSecurityProperties(
            boolean enabled, String issuer, String audience,
            Duration accessTokenTtl, String secret) {
        this(enabled, issuer, audience, accessTokenTtl, secret, Duration.ofHours(8));
    }

    @ConstructorBinding
    public AdminJwtSecurityProperties {
        issuer = issuer == null || issuer.isBlank() ? "corretora-admin" : issuer;
        audience = audience == null || audience.isBlank() ? "corretora-admin-api" : audience;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(10) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofHours(8) : refreshTokenTtl;
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("Admin JWT access token TTL must be positive");
        }
        if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("Admin JWT refresh token TTL must be positive");
        }
    }
}
