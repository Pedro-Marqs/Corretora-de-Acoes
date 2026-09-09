package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsMarketDataPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetCatalogServiceTests {
    private static final UUID ASSET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    BrazilMarketDataPort brazil = mock(BrazilMarketDataPort.class);
    UsMarketDataPort unitedStates = mock(UsMarketDataPort.class);
    MarketCachePersistenceService cache = mock(MarketCachePersistenceService.class);
    MarketDataFreshness freshness = mock(MarketDataFreshness.class);
    ExchangeRateService exchange = mock(ExchangeRateService.class);
    AssetCatalogService service;

    @BeforeEach void setUp() {
        service = new AssetCatalogService(brazil, unitedStates, cache, freshness, exchange);
    }

    @Test void brazilIsOnlineFirstAndPersistsValidResponse() {
        MarketQuote external = quote(Market.BR, Currency.BRL, "10.00");
        CachedAssetQuote cached = cached(Market.BR, Currency.BRL, "10.00");
        when(brazil.findQuote("PETR4")).thenReturn(external);
        when(cache.store(external, freshness)).thenReturn(cached);
        AssetPriceView result = service.find("petr4", Market.BR);
        assertThat(result.priceBrl()).isEqualByComparingTo("10.00");
        assertThat(result.assetId()).isEqualTo(ASSET_ID);
        verify(cache).store(external, freshness);
    }

    @Test void brazilFallsBackWithoutOverwritingCache() {
        when(brazil.findQuote("PETR4")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "brapi", "timeout"));
        when(cache.find("PETR4", Market.BR, freshness)).thenReturn(cached(Market.BR, Currency.BRL, "9.00"));
        AssetPriceView result = service.find("PETR4", Market.BR);
        assertThat(result.originalPrice()).isEqualByComparingTo("9.00");
        assertThat(result.assetId()).isEqualTo(ASSET_ID);
        verify(cache, never()).store(any(MarketQuote.class), any(MarketDataFreshness.class));
    }

    @Test void brazilFailureWithoutCacheBlocksFinancialResult() {
        when(brazil.findQuote("UNKNOWN")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.NOT_FOUND, "brapi", "missing"));
        assertThatThrownBy(() -> service.find("UNKNOWN", Market.BR))
                .isInstanceOf(ExternalDependencyException.class);
    }

    @Test void cachedUsConsultationDoesNotConsumeMarketProviderAndUsesHalfUpConversion() {
        when(cache.find("AAPL", Market.US, freshness)).thenReturn(cached(Market.US, Currency.USD, "10.005"));
        when(exchange.resolveUsdBrl()).thenReturn(new CachedExchangeRate(new BigDecimal("5.00"),
                "awesome", Instant.EPOCH, Instant.EPOCH, false));
        assertThat(service.find("AAPL", Market.US).priceBrl()).isEqualByComparingTo("50.05");
        assertThat(service.find("AAPL", Market.US).assetId()).isEqualTo(ASSET_ID);
        verifyNoInteractions(brazil, unitedStates);
    }

    @Test void usCacheMissLoadsValidSnapshotAndReturnsUsdBrlConversion() {
        MarketQuote external = usQuote("10.005");
        CachedAssetQuote stored = cached(Market.US, Currency.USD, "10.005");
        when(unitedStates.findQuote("AAPL")).thenReturn(external);
        when(cache.store(external, freshness)).thenReturn(stored);
        when(exchange.resolveUsdBrl()).thenReturn(new CachedExchangeRate(new BigDecimal("5.00"),
                "awesome", Instant.EPOCH, Instant.EPOCH, false));

        AssetPriceView result = service.find("aapl", Market.US);

        assertThat(result.originalPrice()).isEqualByComparingTo("10.005");
        assertThat(result.priceBrl()).isEqualByComparingTo("50.05");
        assertThat(result.assetId()).isEqualTo(ASSET_ID);
        verify(cache).store(external, freshness);
    }

    @Test void usConversionUsesFinancialAmountRoundingForPriceAndRate() {
        when(cache.find("AAPL", Market.US, freshness))
                .thenReturn(cached(Market.US, Currency.USD, "10.005"));
        when(exchange.resolveUsdBrl()).thenReturn(new CachedExchangeRate(
                new BigDecimal("5.005"), "awesome", Instant.EPOCH, Instant.EPOCH, false));

        AssetPriceView result = service.find("AAPL", Market.US);

        assertThat(result.priceBrl()).isEqualByComparingTo("50.15");
    }

    @Test void usExternalFailureUsesCacheThatBecameAvailable() {
        CachedAssetQuote fallback = cached(Market.US, Currency.USD, "9.00");
        when(cache.find("AAPL", Market.US, freshness)).thenReturn(null, fallback);
        when(unitedStates.findQuote("AAPL")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "twelve-data", "timeout"));
        when(exchange.resolveUsdBrl()).thenReturn(new CachedExchangeRate(new BigDecimal("5.00"),
                "awesome", Instant.EPOCH, Instant.EPOCH, false));

        assertThat(service.find("AAPL", Market.US).originalPrice()).isEqualByComparingTo("9.00");
        verify(cache, never()).store(any(MarketQuote.class), any(MarketDataFreshness.class));
    }

    @Test void usExternalFailureWithoutCacheReturnsSafeDependencyError() {
        when(unitedStates.findQuote("AAPL")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "twelve-data", "sensitive detail"));

        assertThatThrownBy(() -> service.find("AAPL", Market.US))
                .isInstanceOf(ExternalDependencyException.class);
        verifyNoInteractions(exchange);
    }

    @Test void nullMarketIsRejectedInsteadOfBeingTreatedAsUs() {
        assertThatThrownBy(() -> service.find("AAPL", null))
                .isInstanceOf(MarketDataUnavailableException.class);
        verifyNoInteractions(brazil, cache, exchange);
    }

    private static MarketQuote quote(Market market, Currency currency, String price) {
        return new MarketQuote("PETR4", "Asset", market, currency, new BigDecimal(price),
                Instant.EPOCH, Instant.EPOCH, "provider");
    }
    private static MarketQuote usQuote(String price) {
        return new MarketQuote("AAPL", "Apple Inc.", Market.US, Currency.USD,
                new BigDecimal(price), Instant.EPOCH, Instant.EPOCH, "twelve-data");
    }
    private static CachedAssetQuote cached(Market market, Currency currency, String price) {
        return new CachedAssetQuote(ASSET_ID, market == Market.US ? "AAPL" : "PETR4", "Asset", market,
                currency, new BigDecimal(price), "cache", Instant.EPOCH, Instant.EPOCH, true);
    }
}
