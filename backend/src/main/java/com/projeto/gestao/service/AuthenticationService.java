package com.projeto.gestao.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.Instant;

import com.projeto.gestao.api.controller.LoginRequest;
import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.JwtTokenService;
import com.projeto.gestao.config.JwtSecurityProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final JwtSecurityProperties jwtProperties;
    private final ObjectProvider<JwtTokenService> jwtTokenServiceProvider;
    private final ObjectProvider<RefreshTokenService> refreshTokenServiceProvider;
    private final String dummyPasswordHash;

    public AuthenticationService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            JwtSecurityProperties jwtProperties,
            ObjectProvider<JwtTokenService> jwtTokenServiceProvider,
            ObjectProvider<RefreshTokenService> refreshTokenServiceProvider) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.jwtProperties = jwtProperties;
        this.jwtTokenServiceProvider = jwtTokenServiceProvider;
        this.refreshTokenServiceProvider = refreshTokenServiceProvider;
        this.dummyPasswordHash = passwordEncoder.encode("DummyTimingPassword1!");
    }

    @Transactional
    public Optional<IssuedCredentials> login(LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String email = request.email().toLowerCase(Locale.ROOT);
        Account account = accountRepository
                .findByEmailIgnoreCaseAndStatus(email, AccountStatus.ACTIVE)
                .orElse(null);
        String storedHash = account == null ? dummyPasswordHash : account.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), storedHash);
        if (account == null || !passwordMatches) {
            throw new AuthenticationException();
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AccountPrincipal(account.getId()), null, List.of());
        if (!jwtProperties.enabled() || jwtProperties.sessionCompatibility()) {
            servletRequest.getSession(true);
            sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (!jwtProperties.enabled() || jwtProperties.sessionCompatibility()) {
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
        }
        JwtTokenService jwtTokenService = jwtTokenServiceProvider.getIfAvailable();
        if (jwtTokenService == null) {
            return Optional.empty();
        }
        AccountPrincipal principal = new AccountPrincipal(account.getId());
        RefreshTokenService refreshTokenService = refreshTokenServiceProvider.getIfAvailable();
        if (refreshTokenService == null) {
            throw new IllegalStateException("Refresh token service is not configured");
        }
        JwtTokenService.IssuedJwt accessToken = jwtTokenService.issue(principal);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(principal);
        return Optional.of(new IssuedCredentials(accessToken, refreshToken));
    }

    public record IssuedCredentials(
            JwtTokenService.IssuedJwt accessToken,
            RefreshTokenService.IssuedRefreshToken refreshToken) {

        public String accessTokenValue() {
            return accessToken.tokenValue();
        }

        public Instant accessTokenExpiresAt() {
            return accessToken.expiresAt();
        }
    }
}
