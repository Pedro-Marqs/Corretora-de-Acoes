package com.projeto.gestao.domain.port;

import com.projeto.gestao.domain.model.MarketQuote;

public interface BrazilMarketDataPort {
    MarketQuote findQuote(String ticker);
}
