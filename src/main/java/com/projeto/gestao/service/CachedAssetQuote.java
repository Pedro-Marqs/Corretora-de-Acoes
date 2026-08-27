package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Instant;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record CachedAssetQuote(String ticker, String name, Market market, Currency currency,
        BigDecimal price, String source, Instant quotedAt, Instant collectedAt, boolean stale) { }
