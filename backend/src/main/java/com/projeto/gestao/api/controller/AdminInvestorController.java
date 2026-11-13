package com.projeto.gestao.api.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.dto.AdminInvestorPage;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.service.AdminAuditService;
import com.projeto.gestao.service.AdminInvestorService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@Validated
@RequestMapping("/api/admin/investors")
public class AdminInvestorController {
    private static final String SORT_PATTERN =
            "(id|name|email|status|createdAt|inactivatedAt),(asc|desc)";

    private final AdminInvestorService investorService;

    public AdminInvestorController(AdminInvestorService investorService) {
        this.investorService = investorService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and principal instanceof T(com.projeto.gestao.security.AdminPrincipal)")
    AdminInvestorPage list(
            @RequestParam(defaultValue = "ALL")
            @Pattern(regexp = "ACTIVE|INACTIVE|ALL") String status,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inactivatedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inactivatedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc")
            @Pattern(regexp = SORT_PATTERN) String sort,
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationHeader) {
        String[] sortParts = sort.split(",", -1);
        AccountStatus accountStatus = "ALL".equals(status)
                ? null : AccountStatus.valueOf(status);
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);
        Sort requestedSort = Sort.by(new Sort.Order(direction, sortParts[0]),
                new Sort.Order(Sort.Direction.ASC, "id"));
        return investorService.list(accountStatus, search, createdFrom, createdTo,
                inactivatedFrom, inactivatedTo, PageRequest.of(page, size, requestedSort),
                principal.adminId(), AdminAuditService.correlationId(correlationHeader));
    }

    @PatchMapping("/{accountId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') and principal instanceof T(com.projeto.gestao.security.AdminPrincipal)")
    void changeStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody AdminInvestorStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationHeader) {
        investorService.changeStatus(accountId, AccountStatus.valueOf(request.status().name()),
                principal.adminId(), AdminAuditService.correlationId(correlationHeader));
    }
}
