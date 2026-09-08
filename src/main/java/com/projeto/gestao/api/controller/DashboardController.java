package com.projeto.gestao.api.controller;

import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    DashboardResponse dashboard(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(name = "brokerAssociationId", required = false)
            java.util.UUID brokerAssociationId) {
        return DashboardResponse.from(service.query(principal.accountId(), brokerAssociationId));
    }
}
