package com.projeto.gestao.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.security.AdminPrincipal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminJwtSecurityConfiguration {
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Duration CLOCK_SKEW = Duration.ZERO;
    private static final Set<String> ALLOWED_CLAIMS = Set.of(
            "iss", "aud", "sub", "iat", "exp", "jti", "principal_type", "role");

    @Bean(name = "adminJwtSecretKey")
    SecretKey adminJwtSecretKey(AdminJwtSecurityProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException(
                    "APP_ADMIN_JWT_SECRET must be configured when admin JWT is enabled");
        }
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(properties.secret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_ADMIN_JWT_SECRET must be Base64 encoded", exception);
        }
        if (decoded.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "APP_ADMIN_JWT_SECRET must contain at least 32 decoded bytes");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean(name = "adminJwtEncoder")
    JwtEncoder adminJwtEncoder(SecretKey adminJwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(adminJwtSecretKey));
    }

    @Bean(name = "adminJwtDecoder")
    JwtDecoder adminJwtDecoder(SecretKey adminJwtSecretKey,
            AdminJwtSecurityProperties properties, AdminUserRepository adminUserRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(adminJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(properties.issuer()),
                new JwtTimestampValidator(CLOCK_SKEW),
                new AdminClaimsValidator(properties, Clock.systemUTC()),
                new ActiveAdminValidator(adminUserRepository)));
        return decoder;
    }

    @Bean
    AdminJwtAuthenticationConverter adminJwtAuthenticationConverter() {
        return new AdminJwtAuthenticationConverter();
    }

    public static final class AdminJwtAuthenticationConverter
            implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            return new UsernamePasswordAuthenticationToken(
                    new AdminPrincipal(UUID.fromString(jwt.getSubject())), jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }
    }

    private static final class AdminClaimsValidator implements OAuth2TokenValidator<Jwt> {
        private final AdminJwtSecurityProperties properties;
        private final Clock clock;

        private AdminClaimsValidator(AdminJwtSecurityProperties properties, Clock clock) {
            this.properties = properties;
            this.clock = clock;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            boolean exactAudience = jwt.getAudience().size() == 1
                    && jwt.getAudience().contains(properties.audience());
            boolean admin = "ADMIN".equals(jwt.getClaimAsString("principal_type"));
            boolean role = "ADMIN".equals(jwt.getClaimAsString("role"));
            boolean subjectIsUuid = isUuid(jwt.getSubject());
            boolean requiredTimes = hasConsistentTimes(jwt);
            boolean validJti = isUuid(jwt.getId());
            boolean claimsAreAllowed = ALLOWED_CLAIMS.containsAll(jwt.getClaims().keySet());
            if (exactAudience && admin && role && subjectIsUuid && requiredTimes
                    && validJti && claimsAreAllowed) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT claims are not valid for an administrator", null));
        }

        private boolean isUuid(String value) {
            try {
                UUID uuid = UUID.fromString(value);
                return uuid.toString().equalsIgnoreCase(value);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return false;
            }
        }

        private boolean hasConsistentTimes(Jwt jwt) {
            Instant issuedAt = jwt.getIssuedAt();
            Instant expiresAt = jwt.getExpiresAt();
            if (issuedAt == null || expiresAt == null || !expiresAt.isAfter(issuedAt)) {
                return false;
            }
            return !issuedAt.isAfter(clock.instant().plus(CLOCK_SKEW));
        }
    }

    private static final class ActiveAdminValidator implements OAuth2TokenValidator<Jwt> {
        private final AdminUserRepository adminUserRepository;

        private ActiveAdminValidator(AdminUserRepository adminUserRepository) {
            this.adminUserRepository = adminUserRepository;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            try {
                UUID adminId = UUID.fromString(jwt.getSubject());
                if (adminUserRepository.findByIdAndStatus(adminId, AdminStatus.ACTIVE).isPresent()) {
                    return OAuth2TokenValidatorResult.success();
                }
            } catch (IllegalArgumentException | NullPointerException exception) {
                // The claims validator reports the public authentication failure.
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT administrator is not active", null));
        }
    }
}
