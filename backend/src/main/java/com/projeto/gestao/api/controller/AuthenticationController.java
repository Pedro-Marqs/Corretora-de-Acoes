package com.projeto.gestao.api.controller;

import com.projeto.gestao.service.AuthenticationService;
import com.projeto.gestao.service.RefreshTokenService;
import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.config.JwtSecurityProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final ObjectProvider<RefreshTokenService> refreshTokenServiceProvider;
    private final JwtSecurityProperties jwtProperties;
    private final boolean secureCookie;
    private static final String REFRESH_COOKIE = "REFRESH_TOKEN";

    public AuthenticationController(
            AuthenticationService authenticationService,
            ObjectProvider<RefreshTokenService> refreshTokenServiceProvider,
            JwtSecurityProperties jwtProperties,
            @Value("${app.security.session-cookie-secure:true}") boolean secureCookie) {
        this.authenticationService = authenticationService;
        this.refreshTokenServiceProvider = refreshTokenServiceProvider;
        this.jwtProperties = jwtProperties;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        authenticationService.login(request, servletRequest, servletResponse)
                .ifPresent(issued -> {
                    servletResponse.setHeader(HttpHeaders.AUTHORIZATION,
                            "Bearer " + issued.accessTokenValue());
                    addRefreshCookie(servletResponse, issued.refreshToken().tokenValue());
                });
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken,
            HttpServletResponse response) {
        RefreshTokenService refreshTokenService = refreshTokenService();
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + rotated.accessToken().tokenValue());
        addRefreshCookie(response, rotated.tokenValue());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        RefreshTokenService refreshTokenService = refreshTokenServiceProvider.getIfAvailable();
        if (refreshTokenService != null) {
            refreshTokenService.revokePresented(request.getCookies() == null ? null
                    : java.util.Arrays.stream(request.getCookies())
                            .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
                            .map(jakarta.servlet.http.Cookie::getValue)
                            .findFirst().orElse(null));
            response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString());
        }
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("SESSION", "")
                .path("/").httpOnly(true).secure(secureCookie).sameSite("Lax").maxAge(0).build().toString());
    }

    private RefreshTokenService refreshTokenService() {
        RefreshTokenService service = refreshTokenServiceProvider.getIfAvailable();
        if (service == null) {
            throw new AuthenticationException();
        }
        return service;
    }

    private void addRefreshCookie(HttpServletResponse response, String value) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(REFRESH_COOKIE, value)
                .path("/api/auth").httpOnly(true).secure(secureCookie).sameSite("Lax")
                .maxAge(jwtProperties.refreshTokenTtl()).build().toString());
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .path("/api/auth").httpOnly(true).secure(secureCookie).sameSite("Lax")
                .maxAge(0).build();
    }
}
