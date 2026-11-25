package com.projeto.gestao.service;

import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ExchangeRateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateService.class);
    private final UsdBrlExchangeRatePort external;
    private final MarketCachePersistenceService cache;
    private final MarketDataFreshness freshness;
    private final ConcurrentMap<String, CompletableFuture<CachedExchangeRate>> refreshes;

    @Autowired
    public ExchangeRateService(UsdBrlExchangeRatePort external, MarketCachePersistenceService cache,
            MarketDataFreshness freshness) {
        this(external, cache, freshness, new ConcurrentHashMap<>());
    }

    ExchangeRateService(UsdBrlExchangeRatePort external, MarketCachePersistenceService cache,
            MarketDataFreshness freshness,
            ConcurrentMap<String, CompletableFuture<CachedExchangeRate>> refreshes) {
        this.external = external;
        this.cache = cache;
        this.freshness = freshness;
        this.refreshes = refreshes;
    }

    public CachedExchangeRate resolveUsdBrl() {
        CompletableFuture<CachedExchangeRate> refresh = new CompletableFuture<>();
        CompletableFuture<CachedExchangeRate> running = refreshes.putIfAbsent("USD_BRL", refresh);
        if (running != null) {
            return await(running);
        }

        try {
            CachedExchangeRate cached = cache.findExchangeRate(freshness);
            if (isFresh(cached)) {
                refresh.complete(cached);
                LOGGER.debug("market-data provider={} decision=CACHE_HIT operation=USD_BRL", "cache");
                return cached;
            }

            MarketDataRefreshLock.USD_BRL.lock();
            try {
                cached = cache.findExchangeRate(freshness);
                if (isFresh(cached)) {
                    refresh.complete(cached);
                    LOGGER.debug("market-data provider={} decision=CACHE_HIT operation=USD_BRL", "cache");
                    return cached;
                }
                try {
                    CachedExchangeRate refreshed = cache.store(external.currentRate(), freshness);
                    refresh.complete(refreshed);
                    LOGGER.info("market-data provider={} decision=REFRESHED operation=USD_BRL", "external");
                    return refreshed;
                } catch (ExternalDataFailure | IllegalArgumentException exception) {
                    CachedExchangeRate fallback = cache.findExchangeRate(freshness);
                    if (fallback != null) {
                        refresh.complete(fallback);
                        logFailure(exception, "STALE_FALLBACK");
                        return fallback;
                    }
                    LOGGER.error("market-data provider={} reason={} decision=BLOCKED operation=USD_BRL status={}",
                            sourceOf(exception), reasonOf(exception), statusOf(exception));
                    ExternalDependencyException blocked = new ExternalDependencyException();
                    refresh.completeExceptionally(blocked);
                    throw blocked;
                }
            } finally {
                MarketDataRefreshLock.USD_BRL.unlock();
            }
        } catch (RuntimeException | Error failure) {
            refresh.completeExceptionally(failure);
            throw failure;
        } finally {
            refreshes.remove("USD_BRL", refresh);
        }
    }

    private static CachedExchangeRate await(CompletableFuture<CachedExchangeRate> refresh) {
        try {
            return refresh.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new ExternalDependencyException();
        }
    }

    private static boolean isFresh(CachedExchangeRate cached) {
        return cached != null && !cached.stale();
    }

    private static void logFailure(Exception failure, String decision) {
        LOGGER.warn("market-data provider={} reason={} decision={} operation=USD_BRL status={}",
                sourceOf(failure), reasonOf(failure), decision, statusOf(failure));
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
