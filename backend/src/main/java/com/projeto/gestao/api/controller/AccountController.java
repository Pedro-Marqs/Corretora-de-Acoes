package com.projeto.gestao.api.controller;

import com.projeto.gestao.service.AccountRegistrationService;
import com.projeto.gestao.service.AccountManagementService;
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountRegistrationService registrationService;
    private final AccountManagementService managementService;
    private final boolean secureCookie;

    public AccountController(
            AccountRegistrationService registrationService,
            AccountManagementService managementService,
            @Value("${app.security.session-cookie-secure:true}") boolean secureCookie) {
        this.registrationService = registrationService;
        this.managementService = managementService;
        this.secureCookie = secureCookie;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateAccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return registrationService.register(request);
    }

    @GetMapping("/me")
    AccountDetailsResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return managementService.details(principal.accountId());
    }

    @PatchMapping("/me/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeEmail(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangeEmailRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        managementService.changeEmail(principal.accountId(), request);
        clearCurrentSession(servletRequest, servletResponse);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        managementService.changePassword(principal.accountId(), request);
        clearCurrentSession(servletRequest, servletResponse);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        managementService.inactivate(principal.accountId(), request);
        clearCurrentSession(servletRequest, servletResponse);
    }

    @PostMapping("/reactivation/check")
    ReactivationCheckResponse checkReactivation(@Valid @RequestBody ReactivationRequest request) {
        return managementService.checkReactivation(request);
    }

    @PostMapping("/reactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reactivate(@Valid @RequestBody ReactivationRequest request) {
        managementService.reactivate(request);
    }

    private void clearCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        SecurityContextHolder.clearContext();
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("SESSION", "")
                .path("/").httpOnly(true).secure(secureCookie).sameSite("Lax").maxAge(0).build().toString());
    }
}
