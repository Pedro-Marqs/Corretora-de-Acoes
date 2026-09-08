package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSnapshot(
        BigDecimal availableBalanceBrl,
        List<DashboardPositionView> positions,
        BigDecimal positionsMarketValueBrl,
        BigDecimal patrimonyBrl,
        BigDecimal realizedResultBrl,
        BigDecimal unrealizedResultBrl,
        BigDecimal totalResultBrl,
        DashboardExchangeRateView exchangeRate,
        List<DashboardWarningView> warnings) { }
