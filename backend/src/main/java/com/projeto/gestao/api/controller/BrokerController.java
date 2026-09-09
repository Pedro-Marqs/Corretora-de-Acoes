package com.projeto.gestao.api.controller;

import java.util.List;
import java.util.UUID;

import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.BrokerManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/brokers")
public class BrokerController {

    private final BrokerManagementService service;

    public BrokerController(BrokerManagementService service) {
        this.service = service;
    }

    @GetMapping("/search")
    BrokerLookupResponse lookup(
            @RequestParam
            @Pattern(regexp = "(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})",
                    message = "CNPJ deve possuir 14 dígitos.")
            String cnpj) {
        return BrokerLookupResponse.from(service.lookup(cnpj));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BrokerAssociationResponse associate(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody BrokerAssociationRequest request) {
        return BrokerAssociationResponse.from(service.associate(principal.accountId(), request.cnpj()));
    }

    @GetMapping
    List<BrokerAssociationResponse> list(@AuthenticationPrincipal AccountPrincipal principal) {
        return service.listActive(principal.accountId()).stream()
                .map(BrokerAssociationResponse::from)
                .toList();
    }

    @DeleteMapping("/{associationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable UUID associationId) {
        service.remove(principal.accountId(), associationId);
    }
}
