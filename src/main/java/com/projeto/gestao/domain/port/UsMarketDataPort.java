package com.projeto.gestao.domain.port;

import com.projeto.gestao.domain.model.MarketQuote;

public interface UsMarketDataPort {
    MarketQuote findQuote(String ticker);
}
