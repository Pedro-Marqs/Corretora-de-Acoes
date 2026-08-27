package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.time.Instant;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.service.AssetPriceView;

public record AssetPriceResponse(String ticker, String name, Market market, Currency currency,
        BigDecimal originalPrice, BigDecimal priceBrl, String quoteSource, Instant quoteQuotedAt,
        boolean quoteStale, BigDecimal usdBrlRate, String exchangeRateSource,
        Instant exchangeRateQuotedAt, Boolean exchangeRateStale) {
    static AssetPriceResponse from(AssetPriceView view) {
        return new AssetPriceResponse(view.ticker(), view.name(), view.market(), view.currency(),
                view.originalPrice(), view.priceBrl(), view.quoteSource(), view.quoteQuotedAt(),
                view.quoteStale(), view.usdBrlRate(), view.exchangeRateSource(),
                view.exchangeRateQuotedAt(), view.exchangeRateStale());
    }
}
