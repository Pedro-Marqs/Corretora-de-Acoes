package com.projeto.gestao.domain.dto;

import com.projeto.gestao.domain.model.Market;

public record AdminDashboardRanking(
        String ticker,
        Market market,
        long tradedShares) { }
