package com.projeto.gestao.service;

import java.util.List;

import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsMarketDataPort;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import com.projeto.gestao.repository.AssetRepository;
import org.springframework.stereotype.Service;

@Service
public class MarketRefreshService {
    private final AssetRepository assets;
    private final BrazilMarketDataPort brazil;
    private final UsMarketDataPort unitedStates;
    private final UsdBrlExchangeRatePort exchangeRates;
    private final MarketCachePersistenceService cache;
    private final MarketDataFreshness freshness;

    public MarketRefreshService(AssetRepository assets, BrazilMarketDataPort brazil,
            UsMarketDataPort unitedStates, UsdBrlExchangeRatePort exchangeRates,
            MarketCachePersistenceService cache, MarketDataFreshness freshness) {
        this.assets = assets;
        this.brazil = brazil;
        this.unitedStates = unitedStates;
        this.exchangeRates = exchangeRates;
        this.cache = cache;
        this.freshness = freshness;
    }

    public void refreshBrazilianQuotes() {
        refreshQuotes(Market.BR, brazil::findQuote);
    }

    public void refreshUnitedStatesQuotes() {
        refreshQuotes(Market.US, unitedStates::findQuote);
    }

    public void refreshUsdBrl() {
        try {
            cache.store(exchangeRates.currentRate(), freshness);
        } catch (ExternalDataFailure | IllegalArgumentException exception) {
            cache.markExchangeRateStale();
        }
    }

    private void refreshQuotes(Market market, QuoteLoader loader) {
        List<Asset> positionedAssets = assets.findDistinctActiveWithPositivePositionByMarket(market);
        for (Asset asset : positionedAssets) {
            try {
                cache.store(loader.load(asset.getTicker()), freshness);
            } catch (ExternalDataFailure | IllegalArgumentException exception) {
                cache.markQuoteStale(asset.getId());
            }
        }
    }

    @FunctionalInterface
    private interface QuoteLoader {
        MarketQuote load(String ticker);
    }
}
