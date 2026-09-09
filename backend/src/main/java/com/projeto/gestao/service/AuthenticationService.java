package com.projeto.gestao.service;

import java.util.List;
import java.util.Locale;

import com.projeto.gestao.api.controller.LoginRequest;
import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final String dummyPasswordHash;

    public AuthenticationService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.dummyPasswordHash = passwordEncoder.encode("DummyTimingPassword1!");
    }

    @Transactional(readOnly = true)
    public void login(LoginRequest request, HttpServletRequest servletRequest,
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
        servletRequest.getSession(true);
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
    }
}
