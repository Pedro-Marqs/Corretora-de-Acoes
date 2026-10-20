package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.AssetStatus;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketCachePersistenceServiceUnitTests {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Mock AssetRepository assets;
    @Mock QuoteRepository quotes;
    @Mock ExchangeRateRepository rates;
    @Mock MarketDataFreshness freshness;

    private MarketCachePersistenceService cache;

    @BeforeEach
    void setUp() {
        cache = new MarketCachePersistenceService(assets, quotes, rates);
    }

    @Test
    void rejectsEveryInvalidMarketQuoteFieldAndMarketCurrencyCombination() {
        MarketQuote valid = quote("PETR4", "Petrobras", Market.BR, Currency.BRL,
                amount("10"), "source");
        MarketQuote[] invalid = {
            null,
            new MarketQuote(null, valid.name(), valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(" ", valid.name(), valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), null, valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), " ", valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), null, valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), null, valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), valid.currency(), null,
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), valid.currency(),
                    amount("0"), valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), valid.currency(), valid.price(),
                    null, valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), null, valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), null),
            new MarketQuote(valid.ticker(), valid.name(), valid.market(), valid.currency(), valid.price(),
                    valid.quotedAt(), valid.collectedAt(), " "),
            new MarketQuote(valid.ticker(), valid.name(), Market.BR, Currency.USD, valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source()),
            new MarketQuote(valid.ticker(), valid.name(), Market.US, Currency.BRL, valid.price(),
                    valid.quotedAt(), valid.collectedAt(), valid.source())
        };

        for (MarketQuote candidate : invalid) {
            assertThatThrownBy(() -> cache.store(candidate, freshness))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsEveryInvalidExchangeRateField() {
        UsdBrlRate valid = new UsdBrlRate(amount("5"), NOW, NOW, "source");
        UsdBrlRate[] invalid = {
            null,
            new UsdBrlRate(null, valid.quotedAt(), valid.collectedAt(), valid.source()),
            new UsdBrlRate(amount("0"), valid.quotedAt(), valid.collectedAt(), valid.source()),
            new UsdBrlRate(valid.rate(), null, valid.collectedAt(), valid.source()),
            new UsdBrlRate(valid.rate(), valid.quotedAt(), null, valid.source()),
            new UsdBrlRate(valid.rate(), valid.quotedAt(), valid.collectedAt(), null),
            new UsdBrlRate(valid.rate(), valid.quotedAt(), valid.collectedAt(), " ")
        };

        for (UsdBrlRate candidate : invalid) {
            assertThatThrownBy(() -> cache.store(candidate, freshness))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findReturnsNullForMissingInactiveOrUnquotedAssets() {
        when(assets.findByTickerIgnoreCaseAndMarket("PETR4", Market.BR))
                .thenReturn(Optional.empty());
        assertThat(cache.find("PETR4", Market.BR, freshness)).isNull();

        Asset inactive = org.mockito.Mockito.mock(Asset.class);
        when(inactive.getStatus()).thenReturn(AssetStatus.INACTIVE);
        when(assets.findByTickerIgnoreCaseAndMarket("VALE3", Market.BR))
                .thenReturn(Optional.of(inactive));
        assertThat(cache.find("VALE3", Market.BR, freshness)).isNull();

        Asset active = org.mockito.Mockito.mock(Asset.class);
        UUID assetId = UUID.randomUUID();
        when(active.getStatus()).thenReturn(AssetStatus.ACTIVE);
        when(active.getId()).thenReturn(assetId);
        when(assets.findByTickerIgnoreCaseAndMarket("ITUB4", Market.BR))
                .thenReturn(Optional.of(active));
        when(quotes.findById(assetId)).thenReturn(Optional.empty());
        assertThat(cache.find("ITUB4", Market.BR, freshness)).isNull();
        assertThat(cache.findExchangeRate(freshness)).isNull();
    }

    private static MarketQuote quote(String ticker, String name, Market market, Currency currency,
            BigDecimal price, String source) {
        return new MarketQuote(ticker, name, market, currency, price, NOW, NOW, source);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
