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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketRefreshService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketRefreshService.class);
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
        if (!MarketDataRefreshLock.USD_BRL.tryLock()) {
            LOGGER.debug("market-data provider=refresh decision=SKIPPED_CONCURRENT operation=USD_BRL");
            return;
        }
        try {
            cache.store(exchangeRates.currentRate(), freshness);
            LOGGER.info("market-data provider=external decision=REFRESHED operation=USD_BRL");
        } catch (ExternalDataFailure | IllegalArgumentException exception) {
            if (cache.findExchangeRate(freshness) != null) {
                LOGGER.warn("market-data provider={} reason={} decision=STALE_FALLBACK operation=USD_BRL status={}",
                        sourceOf(exception), reasonOf(exception), statusOf(exception));
            } else {
                LOGGER.error("market-data provider={} reason={} decision=BLOCKED operation=USD_BRL status={}",
                        sourceOf(exception), reasonOf(exception), statusOf(exception));
            }
            cache.markExchangeRateStale();
        } finally {
            MarketDataRefreshLock.USD_BRL.unlock();
        }
    }

    private void refreshQuotes(Market market, QuoteLoader loader) {
        List<Asset> positionedAssets = assets.findDistinctActiveWithPositivePositionByMarket(market);
        for (Asset asset : positionedAssets) {
            try {
                cache.store(loader.load(asset.getTicker()), freshness);
            } catch (ExternalDataFailure | IllegalArgumentException exception) {
                if (cache.find(asset.getTicker(), market, freshness) != null) {
                    LOGGER.warn("market-data provider={} reason={} decision=STALE_FALLBACK operation=QUOTE status={}",
                            sourceOf(exception), reasonOf(exception), statusOf(exception));
                } else {
                    LOGGER.error("market-data provider={} reason={} decision=BLOCKED operation=QUOTE status={}",
                            sourceOf(exception), reasonOf(exception), statusOf(exception));
                }
                cache.markQuoteStale(asset.getId());
            }
        }
    }

    @FunctionalInterface
    private interface QuoteLoader {
        MarketQuote load(String ticker);
    }

    private static String sourceOf(Exception exception) {
        return exception instanceof ExternalDataFailure failure ? failure.source() : "market-data";
    }

    private static String reasonOf(Exception exception) {
        return exception instanceof ExternalDataFailure failure ? failure.reason().name() : "INVALID_RESPONSE";
    }

    private static Integer statusOf(Exception exception) {
        return exception instanceof ExternalDataFailure failure ? failure.statusCode() : null;
    }
}
