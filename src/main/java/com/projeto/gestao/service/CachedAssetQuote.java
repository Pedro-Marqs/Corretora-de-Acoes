package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record CachedAssetQuote(UUID assetId, String ticker, String name, Market market, Currency currency,
        BigDecimal price, String source, Instant quotedAt, Instant collectedAt, boolean stale) { }
