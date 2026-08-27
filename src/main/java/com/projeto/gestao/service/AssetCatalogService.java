package com.projeto.gestao.service;

import java.math.RoundingMode;
import java.util.Locale;

import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import org.springframework.stereotype.Service;

@Service
public class AssetCatalogService {
    private static final String TICKER_PATTERN = "[A-Z0-9]{1,12}";
    private final BrazilMarketDataPort brazil;
    private final MarketCachePersistenceService cache;
    private final MarketDataFreshness freshness;
    private final ExchangeRateService exchangeRates;

    public AssetCatalogService(BrazilMarketDataPort brazil, MarketCachePersistenceService cache,
            MarketDataFreshness freshness, ExchangeRateService exchangeRates) {
        this.brazil = brazil;
        this.cache = cache;
        this.freshness = freshness;
        this.exchangeRates = exchangeRates;
    }

    public AssetPriceView find(String rawTicker, Market market) {
        if (market == null) throw new MarketDataUnavailableException("Mercado invÃ¡lido.");
        String ticker = normalize(rawTicker);
        CachedAssetQuote quote = market == Market.BR ? findBrazil(ticker) : findUnitedStates(ticker);
        if (market == Market.BR) {
            return new AssetPriceView(quote.ticker(), quote.name(), quote.market(), quote.currency(),
                    quote.price(), quote.price(), quote.source(), quote.quotedAt(), quote.stale(),
                    null, null, null, null);
        }
        CachedExchangeRate rate = exchangeRates.resolveUsdBrl();
        return new AssetPriceView(quote.ticker(), quote.name(), quote.market(), quote.currency(),
                quote.price(), quote.price().multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP),
                quote.source(), quote.quotedAt(), quote.stale(), rate.rate(), rate.source(),
                rate.quotedAt(), rate.stale());
    }

    private CachedAssetQuote findBrazil(String ticker) {
        try {
            return cache.store(brazil.findQuote(ticker), freshness);
        } catch (ExternalDataFailure | IllegalArgumentException exception) {
            CachedAssetQuote fallback = cache.find(ticker, Market.BR, freshness);
            if (fallback != null) return fallback;
            throw new ExternalDependencyException();
        }
    }

    private CachedAssetQuote findUnitedStates(String ticker) {
        CachedAssetQuote result = cache.find(ticker, Market.US, freshness);
        if (result == null) {
            throw new MarketDataUnavailableException("Ativo US nÃ£o disponÃ­vel no catÃ¡logo.");
        }
        return result;
    }

    private static String normalize(String ticker) {
        String normalized = ticker == null ? "" : ticker.strip().toUpperCase(Locale.ROOT);
        if (!normalized.matches(TICKER_PATTERN)) {
            throw new MarketDataUnavailableException("Ticker invÃ¡lido.");
        }
        return normalized;
    }
}
