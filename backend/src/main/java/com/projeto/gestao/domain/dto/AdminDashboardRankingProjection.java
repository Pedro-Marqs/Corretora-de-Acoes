package com.projeto.gestao.domain.dto;

import com.projeto.gestao.domain.model.Market;

public interface AdminDashboardRankingProjection {
    String getTicker();
    Market getMarket();
    Long getTradedShares();
}
