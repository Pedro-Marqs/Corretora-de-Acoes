package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record DashboardPositionView(
        String ticker,
        String name,
        Market market,
        Currency currency,
        String brokerageName,
        long quantity,
        BigDecimal averagePriceBrl,
        BigDecimal totalCostBrl,
        BigDecimal quotePrice,
        BigDecimal quotePriceBrl,
        BigDecimal marketValueBrl,
        BigDecimal unrealizedResultBrl,
        OffsetDateTime quoteQuotedAt,
        boolean quoteStale) { }
