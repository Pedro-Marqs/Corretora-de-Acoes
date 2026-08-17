package com.projeto.gestao.api.controller;

import com.projeto.gestao.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
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
    private final boolean secureCookie;

    public AuthenticationController(
            AuthenticationService authenticationService,
            @Value("${app.security.session-cookie-secure:true}") boolean secureCookie) {
        this.authenticationService = authenticationService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        authenticationService.login(request, servletRequest, servletResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("SESSION", "")
                .path("/").httpOnly(true).secure(secureCookie).sameSite("Lax").maxAge(0).build().toString());
    }
}
