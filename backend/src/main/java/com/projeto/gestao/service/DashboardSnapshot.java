package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DashboardSnapshot(
        BigDecimal availableBalanceBrl,
        boolean balanceShared,
        UUID selectedBrokerAssociationId,
        List<DashboardPositionView> positions,
        BigDecimal positionsMarketValueBrl,
        BigDecimal patrimonyBrl,
        BigDecimal realizedResultBrl,
        BigDecimal unrealizedResultBrl,
        BigDecimal totalResultBrl,
        DashboardDistributionsView distributions,
        DashboardExchangeRateView exchangeRate,
        List<DashboardWarningView> warnings,
        String period,
        List<DashboardPatrimonialPointView> patrimonyHistory) { }
