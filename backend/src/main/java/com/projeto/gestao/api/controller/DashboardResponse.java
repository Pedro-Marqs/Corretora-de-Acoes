package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.service.DashboardDistributionsView;
import com.projeto.gestao.service.DashboardExchangeRateView;
import com.projeto.gestao.service.DashboardInvestmentPointView;
import com.projeto.gestao.service.DashboardPositionView;
import com.projeto.gestao.service.DashboardPatrimonialPointView;
import com.projeto.gestao.service.DashboardSnapshot;
import com.projeto.gestao.service.DashboardWarningView;

public record DashboardResponse(
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
        List<DashboardPatrimonialPointView> patrimonyHistory,
        List<DashboardInvestmentPointView> investmentHistory) {
    static DashboardResponse from(DashboardSnapshot snapshot) {
        return new DashboardResponse(snapshot.availableBalanceBrl(), snapshot.balanceShared(),
                snapshot.selectedBrokerAssociationId(), snapshot.positions(),
                snapshot.positionsMarketValueBrl(), snapshot.patrimonyBrl(),
                snapshot.realizedResultBrl(), snapshot.unrealizedResultBrl(),
                snapshot.totalResultBrl(), snapshot.distributions(), snapshot.exchangeRate(),
                snapshot.warnings(), snapshot.period(), snapshot.patrimonyHistory(), snapshot.investmentHistory());
    }
}
