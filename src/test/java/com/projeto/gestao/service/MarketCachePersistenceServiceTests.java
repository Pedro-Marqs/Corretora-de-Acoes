package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.repository.AssetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketCachePersistenceServiceTests {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private final MarketDataFreshness freshness = new MarketDataFreshness(Clock.fixed(NOW, ZoneOffset.UTC));
    @Autowired MarketCachePersistenceService cache;
    @Autowired AssetRepository assets;

    @Test void storesOneAssetPerTickerAndMarketAndRejectsOlderSnapshot() {
        cache.store(quote("10.00", NOW.minusSeconds(10), "first"), freshness);
        CachedAssetQuote result = cache.store(quote("9.00", NOW.minusSeconds(20), "older"), freshness);

        assertThat(result.price()).isEqualByComparingTo("10.00");
        assertThat(result.source()).isEqualTo("first");
        assertThat(assets.count()).isEqualTo(1);
    }

    @Test void invalidResponseCannotReplaceLastValidQuote() {
        cache.store(quote("10.00", NOW.minusSeconds(10), "valid"), freshness);
        MarketQuote invalid = quote("0", NOW, "invalid");

        assertThatThrownBy(() -> cache.store(invalid, freshness)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cache.find("PETR4", Market.BR, freshness).price()).isEqualByComparingTo("10.00");
    }

    @Test void rejectsCurrencyThatDoesNotBelongToMarket() {
        MarketQuote invalid = new MarketQuote("PETR4", "Petrobras", Market.BR, Currency.USD,
                new BigDecimal("10.00"), NOW, NOW, "invalid");
        assertThatThrownBy(() -> cache.store(invalid, freshness))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(assets.count()).isZero();
    }

    @Test void exchangeRateKeepsNewestValidSnapshot() {
        cache.store(new UsdBrlRate(new BigDecimal("5.20"), NOW, NOW, "awesome"), freshness);
        CachedExchangeRate result = cache.store(new UsdBrlRate(new BigDecimal("4.90"),
                NOW.minusSeconds(1), NOW, "older"), freshness);
        assertThat(result.rate()).isEqualByComparingTo("5.20");
        assertThat(result.source()).isEqualTo("awesome");
    }

    private MarketQuote quote(String price, Instant quotedAt, String source) {
        return new MarketQuote("PETR4", "Petrobras", Market.BR, Currency.BRL,
                new BigDecimal(price), quotedAt, NOW, source);
    }
}
