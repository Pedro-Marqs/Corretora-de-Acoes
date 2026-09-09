package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record AssetPriceView(UUID assetId, String ticker, String name, Market market, Currency currency,
        BigDecimal originalPrice, BigDecimal priceBrl, String quoteSource, Instant quoteQuotedAt,
        boolean quoteStale, BigDecimal usdBrlRate, String exchangeRateSource,
        Instant exchangeRateQuotedAt, Boolean exchangeRateStale) { }
