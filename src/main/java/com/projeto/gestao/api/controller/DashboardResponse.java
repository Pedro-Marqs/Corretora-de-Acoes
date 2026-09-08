package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.util.List;

import com.projeto.gestao.service.DashboardExchangeRateView;
import com.projeto.gestao.service.DashboardPositionView;
import com.projeto.gestao.service.DashboardSnapshot;
import com.projeto.gestao.service.DashboardWarningView;

public record DashboardResponse(
        BigDecimal availableBalanceBrl,
        List<DashboardPositionView> positions,
        BigDecimal positionsMarketValueBrl,
        BigDecimal patrimonyBrl,
        BigDecimal realizedResultBrl,
        BigDecimal unrealizedResultBrl,
        BigDecimal totalResultBrl,
        DashboardExchangeRateView exchangeRate,
        List<DashboardWarningView> warnings) {
    static DashboardResponse from(DashboardSnapshot snapshot) {
        return new DashboardResponse(snapshot.availableBalanceBrl(), snapshot.positions(),
                snapshot.positionsMarketValueBrl(), snapshot.patrimonyBrl(),
                snapshot.realizedResultBrl(), snapshot.unrealizedResultBrl(),
                snapshot.totalResultBrl(), snapshot.exchangeRate(), snapshot.warnings());
    }
}
