package com.projeto.gestao.api.controller;

import java.util.Arrays;

import com.projeto.gestao.config.AdminJwtSecurityProperties;
import com.projeto.gestao.service.AdminAuthenticationService;
import com.projeto.gestao.service.AdminRefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@ConditionalOnProperty(prefix = "app.security.admin-jwt", name = "enabled", havingValue = "true")
public class AdminAuthenticationController {
    static final String REFRESH_COOKIE = "ADMIN_REFRESH_TOKEN";

    private final AdminAuthenticationService authenticationService;
    private final AdminRefreshTokenService refreshTokenService;
    private final AdminJwtSecurityProperties properties;
    private final boolean secureCookie;

    public AdminAuthenticationController(
            AdminAuthenticationService authenticationService,
            AdminRefreshTokenService refreshTokenService,
            AdminJwtSecurityProperties properties,
            @Value("${app.security.session-cookie-secure:true}") boolean secureCookie) {
        this.authenticationService = authenticationService;
        this.refreshTokenService = refreshTokenService;
        this.properties = properties;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AdminAuthenticationService.IssuedCredentials issued = authenticationService.login(request);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + issued.accessTokenValue());
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshCookie(issued.refreshToken().tokenValue()).toString());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String rawRefreshToken,
            HttpServletResponse response) {
        AdminRefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + rotated.accessToken().tokenValue());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(rotated.tokenValue()).toString());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenService.revokePresented(Arrays.stream(
                        request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null));
        response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString());
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .path("/api/admin/auth")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .maxAge(properties.refreshTokenTtl())
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .path("/api/admin/auth")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }
}
