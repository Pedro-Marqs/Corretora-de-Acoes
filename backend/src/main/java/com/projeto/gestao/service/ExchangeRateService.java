package com.projeto.gestao.service;

import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {
    private final UsdBrlExchangeRatePort external;
    private final MarketCachePersistenceService cache;
    private final MarketDataFreshness freshness;

    public ExchangeRateService(UsdBrlExchangeRatePort external, MarketCachePersistenceService cache,
            MarketDataFreshness freshness) {
        this.external = external;
        this.cache = cache;
        this.freshness = freshness;
    }

    public CachedExchangeRate resolveUsdBrl() {
        try {
            return cache.store(external.currentRate(), freshness);
        } catch (ExternalDataFailure | IllegalArgumentException exception) {
            CachedExchangeRate fallback = cache.findExchangeRate(freshness);
            if (fallback != null) return fallback;
            throw new ExternalDependencyException();
        }
    }
}
