package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetCatalogServiceTests {
    BrazilMarketDataPort brazil = mock(BrazilMarketDataPort.class);
    MarketCachePersistenceService cache = mock(MarketCachePersistenceService.class);
    MarketDataFreshness freshness = mock(MarketDataFreshness.class);
    ExchangeRateService exchange = mock(ExchangeRateService.class);
    AssetCatalogService service;

    @BeforeEach void setUp() { service = new AssetCatalogService(brazil, cache, freshness, exchange); }

    @Test void brazilIsOnlineFirstAndPersistsValidResponse() {
        MarketQuote external = quote(Market.BR, Currency.BRL, "10.00");
        CachedAssetQuote cached = cached(Market.BR, Currency.BRL, "10.00");
        when(brazil.findQuote("PETR4")).thenReturn(external);
        when(cache.store(external, freshness)).thenReturn(cached);
        assertThat(service.find("petr4", Market.BR).priceBrl()).isEqualByComparingTo("10.00");
        verify(cache).store(external, freshness);
    }

    @Test void brazilFallsBackWithoutOverwritingCache() {
        when(brazil.findQuote("PETR4")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "brapi", "timeout"));
        when(cache.find("PETR4", Market.BR, freshness)).thenReturn(cached(Market.BR, Currency.BRL, "9.00"));
        assertThat(service.find("PETR4", Market.BR).originalPrice()).isEqualByComparingTo("9.00");
        verify(cache, never()).store(any(MarketQuote.class), any(MarketDataFreshness.class));
    }

    @Test void brazilFailureWithoutCacheBlocksFinancialResult() {
        when(brazil.findQuote("UNKNOWN")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.NOT_FOUND, "brapi", "missing"));
        assertThatThrownBy(() -> service.find("UNKNOWN", Market.BR))
                .isInstanceOf(ExternalDependencyException.class);
    }

    @Test void usConsultationNeverConsumesMarketProviderAndUsesHalfUpConversion() {
        when(cache.find("AAPL", Market.US, freshness)).thenReturn(cached(Market.US, Currency.USD, "10.005"));
        when(exchange.resolveUsdBrl()).thenReturn(new CachedExchangeRate(new BigDecimal("5.00"),
                "awesome", Instant.EPOCH, Instant.EPOCH, false));
        assertThat(service.find("AAPL", Market.US).priceBrl()).isEqualByComparingTo("50.03");
        verifyNoInteractions(brazil);
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
    private static CachedAssetQuote cached(Market market, Currency currency, String price) {
        return new CachedAssetQuote(market == Market.US ? "AAPL" : "PETR4", "Asset", market,
                currency, new BigDecimal(price), "cache", Instant.EPOCH, Instant.EPOCH, true);
    }
}
