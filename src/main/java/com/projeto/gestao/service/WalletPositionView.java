package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record WalletPositionView(
        UUID assetId, String ticker, String name, Market market, Currency currency,
        UUID brokerageId, String brokerageName, long quantity, BigDecimal averagePriceBrl,
        BigDecimal quotePrice, BigDecimal quotePriceBrl, BigDecimal marketValueBrl,
        BigDecimal unrealizedResultBrl, OffsetDateTime quoteQuotedAt, boolean quoteStale,
        BigDecimal usdBrlRate, OffsetDateTime exchangeRateQuotedAt, Boolean exchangeRateStale) { }
