package com.projeto.gestao.api.controller;

import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.DashboardPeriod;
import com.projeto.gestao.service.DashboardService;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Validated
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    DashboardResponse dashboard(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(name = "brokerAssociationId", required = false)
            java.util.UUID brokerAssociationId,
            @RequestParam(name = "period", required = false)
            @Pattern(regexp = "4W|3M|6M|1Y|5Y|MAX", message = "Período inválido.")
            String period) {
        DashboardPeriod selectedPeriod = period == null ? null : DashboardPeriod.from(period);
        return DashboardResponse.from(
                service.query(principal.accountId(), brokerAssociationId, selectedPeriod));
    }
}
