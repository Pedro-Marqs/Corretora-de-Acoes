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
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
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
    @Autowired MovementRepository movements;
    @Autowired PatrimonialPointRepository patrimonialPoints;

    @Test void storesOneAssetPerTickerAndMarketAndRejectsOlderSnapshot() {
        CachedAssetQuote first = cache.store(quote("10.00", NOW.minusSeconds(10), "first"), freshness);
        CachedAssetQuote result = cache.store(quote("9.00", NOW.minusSeconds(20), "older"), freshness);

        assertThat(result.price()).isEqualByComparingTo("10.00");
        assertThat(result.source()).isEqualTo("first");
        assertThat(result.assetId()).isEqualTo(first.assetId())
                .isEqualTo(assets.findByTickerIgnoreCaseAndMarket("PETR4", Market.BR).orElseThrow().getId());
        assertThat(cache.find("PETR4", Market.BR, freshness).assetId()).isEqualTo(first.assetId());
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

    @Test void failedRefreshStatePreservesValuesAndDoesNotCreateFinancialHistory() {
        CachedAssetQuote storedQuote = cache.store(quote("10.00", NOW, "valid"), freshness);
        cache.store(new UsdBrlRate(new BigDecimal("5.20"), NOW, NOW, "awesome"), freshness);
        long movementCount = movements.count();
        long pointCount = patrimonialPoints.count();

        cache.markQuoteStale(assets.findByTickerIgnoreCaseAndMarket("PETR4", Market.BR)
                .orElseThrow().getId());
        cache.markExchangeRateStale();

        CachedAssetQuote quote = cache.find("PETR4", Market.BR, freshness);
        CachedExchangeRate rate = cache.findExchangeRate(freshness);
        assertThat(quote.price()).isEqualByComparingTo(storedQuote.price());
        assertThat(quote.quotedAt()).isEqualTo(storedQuote.quotedAt());
        assertThat(quote.stale()).isTrue();
        assertThat(rate.rate()).isEqualByComparingTo("5.20");
        assertThat(rate.quotedAt()).isEqualTo(NOW);
        assertThat(rate.stale()).isTrue();
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(patrimonialPoints.count()).isEqualTo(pointCount);
    }

    @Test void successfulSnapshotWithSameTimestampClearsStaleState() {
        cache.store(quote("10.00", NOW, "valid"), freshness);
        cache.store(new UsdBrlRate(new BigDecimal("5.20"), NOW, NOW, "awesome"), freshness);
        cache.markQuoteStale(assets.findByTickerIgnoreCaseAndMarket("PETR4", Market.BR)
                .orElseThrow().getId());
        cache.markExchangeRateStale();

        cache.store(quote("10.00", NOW, "valid"), freshness);
        cache.store(new UsdBrlRate(new BigDecimal("5.20"), NOW, NOW, "awesome"), freshness);

        assertThat(cache.find("PETR4", Market.BR, freshness).stale()).isFalse();
        assertThat(cache.findExchangeRate(freshness).stale()).isFalse();
    }

    private MarketQuote quote(String price, Instant quotedAt, String source) {
        return new MarketQuote("PETR4", "Petrobras", Market.BR, Currency.BRL,
                new BigDecimal(price), quotedAt, NOW, source);
    }
}
