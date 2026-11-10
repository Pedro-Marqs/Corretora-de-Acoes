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
import com.projeto.gestao.config.AdminJwtSecurityProperties;
import com.projeto.gestao.domain.model.AdminRefreshToken;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminRefreshTokenService {
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminJwtTokenService jwtTokenService;
    private final AdminJwtSecurityProperties properties;
    private final Clock clock;

    public AdminRefreshTokenService(
            AdminRefreshTokenRepository refreshTokenRepository,
            AdminUserRepository adminUserRepository,
            AdminJwtTokenService jwtTokenService,
            AdminJwtSecurityProperties properties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.adminUserRepository = adminUserRepository;
        this.jwtTokenService = jwtTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(AdminPrincipal principal) {
        OffsetDateTime now = now();
        String rawToken = randomToken();
        AdminRefreshToken token = AdminRefreshToken.issue(
                UUID.randomUUID(), hash(rawToken), principal.adminId(), UUID.randomUUID(),
                now, now.plus(properties.refreshTokenTtl()));
        refreshTokenRepository.saveAndFlush(token);
        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }

    @Transactional(noRollbackFor = {
            AuthenticationException.class, RefreshTokenFamilyRevokedException.class})
    public RotatedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationException();
        }
        AdminRefreshToken current = refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .orElseThrow(AuthenticationException::new);
        OffsetDateTime now = now();
        if (current.getRevokedAt() != null || current.isExpired(now)) {
            if (current.isConsumed()) {
                refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
                throw new RefreshTokenFamilyRevokedException();
            }
            throw new AuthenticationException();
        }

        UUID adminId = adminUserRepository.findByIdAndStatus(
                        current.getAdminUserId(), AdminStatus.ACTIVE)
                .map(com.projeto.gestao.domain.model.AdminUser::getId)
                .orElseThrow(() -> revokeAndReject(current, now));
        String nextRawToken = randomToken();
        AdminRefreshToken next = AdminRefreshToken.issue(
                UUID.randomUUID(), hash(nextRawToken), adminId, current.getFamilyId(),
                now, now.plus(properties.refreshTokenTtl()));
        current.rotateTo(next.getId(), now);
        refreshTokenRepository.save(current);
        refreshTokenRepository.save(next);
        refreshTokenRepository.flush();
        return new RotatedRefreshToken(
                jwtTokenService.issue(new AdminPrincipal(adminId)),
                nextRawToken, next.getExpiresAt());
    }

    @Transactional
    public UUID revokePresented(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        return refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .map(token -> {
                    refreshTokenRepository.revokeFamily(token.getFamilyId(), now());
                    return token.getAdminUserId();
                }).orElse(null);
    }

    @Transactional
    public void revokeAllForAdmin(UUID adminId) {
        refreshTokenRepository.revokeAllForAdmin(adminId, now());
    }

    private AuthenticationException revokeAndReject(
            AdminRefreshToken token, OffsetDateTime occurredAt) {
        refreshTokenRepository.revokeFamily(token.getFamilyId(), occurredAt);
        return new AuthenticationException();
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
            AdminJwtTokenService.IssuedJwt accessToken,
            String tokenValue,
            OffsetDateTime expiresAt) { }
}
