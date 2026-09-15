package com.projeto.gestao.api.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.HistoryQueryService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryQueryService service;

    public HistoryController(HistoryQueryService service) {
        this.service = service;
    }

    @GetMapping
    HistoryResponse history(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Página deve ser zero ou maior.") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false)
            @Pattern(regexp = "(?i)[A-Z0-9.]{1,20}", message = "Ticker inválido.") String ticker,
            @RequestParam(required = false) UUID brokerId,
            @RequestParam(required = false) Market market) {
        return HistoryResponse.from(service.query(principal.accountId(), page, from, to,
                type, ticker, brokerId, market));
    }
}
