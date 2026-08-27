package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(String ticker, String name, Market market, Currency currency,
        BigDecimal price, Instant quotedAt, Instant collectedAt, String source) {
}
