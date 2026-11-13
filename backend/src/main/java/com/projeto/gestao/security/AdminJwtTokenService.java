package com.projeto.gestao.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.config.AdminJwtSecurityProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminJwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final AdminJwtSecurityProperties properties;
    private final Clock clock;

    @Autowired
    public AdminJwtTokenService(
            @org.springframework.beans.factory.annotation.Qualifier("adminJwtEncoder") JwtEncoder jwtEncoder,
            AdminJwtSecurityProperties properties) {
        this(jwtEncoder, properties, Clock.systemUTC());
    }

    AdminJwtTokenService(JwtEncoder jwtEncoder, AdminJwtSecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedJwt issue(AdminPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(principal.adminId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("principal_type", "ADMIN")
                .claim("role", "ADMIN")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        return new IssuedJwt(token, expiresAt);
    }

    public record IssuedJwt(String tokenValue, Instant expiresAt) { }
}
