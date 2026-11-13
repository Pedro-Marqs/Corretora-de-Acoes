package com.projeto.gestao.api.controller;

import com.projeto.gestao.domain.dto.AdminDashboardResponse;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.service.AdminDashboardService;
import com.projeto.gestao.service.DashboardPeriod;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and principal instanceof T(com.projeto.gestao.security.AdminPrincipal)")
    AdminDashboardResponse dashboard(
            @RequestParam @NotBlank
            @Pattern(regexp = "4W|3M|6M|1Y|5Y|MAX") String period,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            AdminPrincipal principal) {
        return dashboardService.query(DashboardPeriod.from(period), limit);
    }
}
