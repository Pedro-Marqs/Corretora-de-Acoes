package com.projeto.gestao.service;

import java.util.Locale;

import com.projeto.gestao.api.controller.LoginRequest;
import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminAuthenticationService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtTokenService jwtTokenService;
    private final AdminRefreshTokenService refreshTokenService;
    private final String dummyPasswordHash;

    public AdminAuthenticationService(
            AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder,
            AdminJwtTokenService jwtTokenService, AdminRefreshTokenService refreshTokenService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("DummyAdminTimingPassword1!");
    }

    @Transactional
    public IssuedCredentials login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(email).orElse(null);
        String storedHash = admin == null ? dummyPasswordHash : admin.passwordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), storedHash);
        if (admin == null || admin.getStatus() != AdminStatus.ACTIVE || !passwordMatches) {
            throw new AuthenticationException();
        }
        AdminPrincipal principal = new AdminPrincipal(admin.getId());
        return new IssuedCredentials(
                jwtTokenService.issue(principal), refreshTokenService.issue(principal));
    }

    public record IssuedCredentials(
            AdminJwtTokenService.IssuedJwt accessToken,
            AdminRefreshTokenService.IssuedRefreshToken refreshToken) {
        public String accessTokenValue() {
            return accessToken.tokenValue();
        }
    }
}
