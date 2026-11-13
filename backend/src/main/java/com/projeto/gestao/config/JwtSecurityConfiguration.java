package com.projeto.gestao.config;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@ConditionalOnProperty(prefix = "app.security.jwt", name = "enabled", havingValue = "true")
public class JwtSecurityConfiguration {
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Duration CLOCK_SKEW = Duration.ZERO;
    private static final Set<String> ALLOWED_CLAIMS = Set.of(
            "iss", "aud", "sub", "iat", "exp", "jti", "principal_type", "role");

    @Bean
    SecretKey jwtSecretKey(JwtSecurityProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET must be configured when JWT is enabled");
        }
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(properties.secret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_JWT_SECRET must be Base64 encoded", exception);
        }
        if (decoded.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("APP_JWT_SECRET must contain at least 32 decoded bytes");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtSecurityProperties properties,
            AccountRepository accountRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(CLOCK_SKEW);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(properties.issuer()), timestampValidator,
                new InvestorClaimsValidator(properties, Clock.systemUTC()),
                new ActiveInvestorAccountValidator(accountRepository)));
        return decoder;
    }

    @Bean
    InvestorJwtAuthenticationConverter jwtAuthenticationConverter() {
        return new InvestorJwtAuthenticationConverter();
    }

    static final class InvestorJwtAuthenticationConverter
            implements Converter<Jwt, AbstractAuthenticationToken> {
        @Override
        public AbstractAuthenticationToken convert(Jwt jwt) {
            return new UsernamePasswordAuthenticationToken(
                    new AccountPrincipal(UUID.fromString(jwt.getSubject())), jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
        }
    }

    private static final class InvestorClaimsValidator implements OAuth2TokenValidator<Jwt> {
        private final JwtSecurityProperties properties;
        private final Clock clock;

        private InvestorClaimsValidator(JwtSecurityProperties properties, Clock clock) {
            this.properties = properties;
            this.clock = clock;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            boolean exactAudience = jwt.getAudience().size() == 1
                    && jwt.getAudience().contains(properties.audience());
            boolean investor = "INVESTOR".equals(jwt.getClaimAsString("principal_type"));
            boolean user = "USER".equals(jwt.getClaimAsString("role"));
            boolean subjectIsUuid = isUuid(jwt.getSubject());
            boolean requiredTimes = hasConsistentTimes(jwt);
            boolean validJti = isUuid(jwt.getId());
            boolean claimsAreAllowed = ALLOWED_CLAIMS.containsAll(jwt.getClaims().keySet());
            if (exactAudience && investor && user && subjectIsUuid && requiredTimes
                    && validJti && claimsAreAllowed) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT claims are not valid for an investor", null));
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
            Instant now = clock.instant();
            return !issuedAt.isAfter(now.plus(CLOCK_SKEW));
        }
    }

    private static final class ActiveInvestorAccountValidator implements OAuth2TokenValidator<Jwt> {
        private final AccountRepository accountRepository;

        private ActiveInvestorAccountValidator(AccountRepository accountRepository) {
            this.accountRepository = accountRepository;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            UUID accountId;
            try {
                accountId = UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException | NullPointerException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "JWT subject is not a valid account", null));
            }
            if (accountRepository.findByIdAndStatus(accountId, AccountStatus.ACTIVE).isPresent()) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "JWT account is not active", null));
        }
    }
}
