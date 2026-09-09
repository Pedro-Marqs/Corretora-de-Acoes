package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record PurchaseQuote(UUID assetId, String ticker, Market market, Currency currency,
        BigDecimal originalPrice, BigDecimal unitPriceBrl, String quoteSource,
        Instant quoteQuotedAt, boolean quoteStale, BigDecimal usdBrlRate,
        String exchangeRateSource, Instant exchangeRateQuotedAt, Boolean exchangeRateStale) { }
