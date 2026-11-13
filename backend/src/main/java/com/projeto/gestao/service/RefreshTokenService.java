package com.projeto.gestao.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.RefreshTokenFamilyRevokedException;
import com.projeto.gestao.config.JwtSecurityProperties;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.RefreshToken;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.RefreshTokenRepository;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.JwtTokenService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.security.jwt", name = "enabled", havingValue = "true")
public class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountRepository accountRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtSecurityProperties jwtProperties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            AccountRepository accountRepository,
            JwtTokenService jwtTokenService,
            JwtSecurityProperties jwtProperties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountRepository = accountRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(AccountPrincipal principal) {
        OffsetDateTime now = now();
        String rawToken = randomToken();
        RefreshToken token = RefreshToken.issue(
                UUID.randomUUID(), hash(rawToken), principal.accountId(), UUID.randomUUID(),
                now, now.plus(jwtProperties.refreshTokenTtl()));
        refreshTokenRepository.saveAndFlush(token);
        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }

    @Transactional(noRollbackFor = RefreshTokenFamilyRevokedException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationException();
        }
        String tokenHash = hash(rawToken);
        RefreshToken current = refreshTokenRepository.findForUpdateByTokenHash(tokenHash)
                .orElseThrow(AuthenticationException::new);
        OffsetDateTime now = now();

        if (current.getRevokedAt() != null || current.isExpired(now)) {
            if (current.isConsumed()) {
                refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
                throw new RefreshTokenFamilyRevokedException();
            }
            throw new AuthenticationException();
        }

        Account account = accountRepository.findByIdAndStatus(current.getAccountId(), AccountStatus.ACTIVE)
                .orElseThrow(() -> revokeAndReject(current, now));

        String nextRawToken = randomToken();
        RefreshToken next = RefreshToken.issue(
                UUID.randomUUID(), hash(nextRawToken), account.getId(), current.getFamilyId(),
                now, now.plus(jwtProperties.refreshTokenTtl()));
        current.rotateTo(next.getId(), now);
        refreshTokenRepository.save(current);
        refreshTokenRepository.save(next);
        refreshTokenRepository.flush();
        return new RotatedRefreshToken(
                jwtTokenService.issue(new AccountPrincipal(account.getId())),
                nextRawToken,
                next.getExpiresAt());
    }

    @Transactional
    public void revokePresented(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken)).ifPresent(token ->
                refreshTokenRepository.revokeFamily(token.getFamilyId(), now()));
    }

    @Transactional
    public void revokeAllForAccount(UUID accountId) {
        refreshTokenRepository.revokeAllForAccount(accountId, now());
    }

    private RefreshTokenFamilyRevokedException revokeAndReject(
            RefreshToken token, OffsetDateTime occurredAt) {
        refreshTokenRepository.revokeFamily(token.getFamilyId(), occurredAt);
        return new RefreshTokenFamilyRevokedException();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(String tokenValue, OffsetDateTime expiresAt) { }

    public record RotatedRefreshToken(
            JwtTokenService.IssuedJwt accessToken,
            String tokenValue,
            OffsetDateTime expiresAt) { }
}
