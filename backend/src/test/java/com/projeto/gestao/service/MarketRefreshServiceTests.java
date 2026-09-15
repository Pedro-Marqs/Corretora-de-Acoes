package com.projeto.gestao.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsMarketDataPort;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import com.projeto.gestao.repository.AssetRepository;
import org.junit.jupiter.api.Test;

class MarketRefreshServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-02T13:00:00Z");
    private final AssetRepository assets = mock(AssetRepository.class);
    private final BrazilMarketDataPort brazil = mock(BrazilMarketDataPort.class);
    private final UsMarketDataPort unitedStates = mock(UsMarketDataPort.class);
    private final UsdBrlExchangeRatePort exchangeRates = mock(UsdBrlExchangeRatePort.class);
    private final MarketCachePersistenceService cache = mock(MarketCachePersistenceService.class);
    private final MarketDataFreshness freshness = mock(MarketDataFreshness.class);
    private final MarketRefreshService service = new MarketRefreshService(assets, brazil, unitedStates,
            exchangeRates, cache, freshness);

    @Test
    void refreshesOnlyDistinctEligibleAssetsReturnedByPositionQuery() {
        Asset positioned = new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL);
        MarketQuote quote = quote("PETR4", Market.BR, Currency.BRL);
        when(assets.findDistinctActiveWithPositivePositionByMarket(Market.BR))
                .thenReturn(List.of(positioned));
        when(brazil.findQuote("PETR4")).thenReturn(quote);

        service.refreshBrazilianQuotes();

        verify(brazil).findQuote("PETR4");
        verify(brazil, never()).findQuote("VALE3");
        verify(cache).store(quote, freshness);
    }

    @Test
    void failedQuoteKeepsCacheAndMarksExistingValueStale() {
        Asset positioned = new Asset("AAPL", "Apple", Market.US, Currency.USD);
        when(assets.findDistinctActiveWithPositivePositionByMarket(Market.US))
                .thenReturn(List.of(positioned));
        when(unitedStates.findQuote("AAPL")).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.SERVER_ERROR, "Twelve Data", "unavailable"));

        service.refreshUnitedStatesQuotes();

        verify(cache, never()).store(org.mockito.ArgumentMatchers.any(MarketQuote.class),
                org.mockito.ArgumentMatchers.any(MarketDataFreshness.class));
        verify(cache).markQuoteStale(positioned.getId());
    }

    @Test
    void failedExchangeRateKeepsCacheAndMarksExistingValueStale() {
        when(exchangeRates.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.SERVER_ERROR, "AwesomeAPI", "unavailable"));

        service.refreshUsdBrl();

        verify(cache, never()).store(org.mockito.ArgumentMatchers.any(UsdBrlRate.class),
                org.mockito.ArgumentMatchers.any(MarketDataFreshness.class));
        verify(cache).markExchangeRateStale();
    }

    private static MarketQuote quote(String ticker, Market market, Currency currency) {
        return new MarketQuote(ticker, ticker, market, currency, BigDecimal.TEN, NOW, NOW, "provider");
    }
}
