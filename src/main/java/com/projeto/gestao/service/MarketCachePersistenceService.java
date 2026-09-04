package com.projeto.gestao.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketCachePersistenceService {
    public static final String USD_BRL = "USD/BRL";
    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();
    private final AssetRepository assets;
    private final QuoteRepository quotes;
    private final ExchangeRateRepository rates;

    public MarketCachePersistenceService(AssetRepository assets, QuoteRepository quotes,
            ExchangeRateRepository rates) {
        this.assets = assets;
        this.quotes = quotes;
        this.rates = rates;
    }

    @Transactional
    public CachedAssetQuote store(MarketQuote incoming, MarketDataFreshness freshness) {
        validate(incoming);
        String key = incoming.market() + ":" + incoming.ticker().toUpperCase();
        synchronized (LOCKS.computeIfAbsent(key, ignored -> new Object())) {
            return storeQuote(incoming, freshness);
        }
    }

    private CachedAssetQuote storeQuote(MarketQuote incoming, MarketDataFreshness freshness) {
        Asset asset = assets.findByTickerIgnoreCaseAndMarket(incoming.ticker(), incoming.market())
                .orElseGet(() -> assets.save(new Asset(incoming.ticker().toUpperCase(),
                        incoming.name(), incoming.market(), incoming.currency())));
        Quote quote = quotes.findByAssetId(asset.getId()).orElse(null);
        OffsetDateTime quotedAt = atUtc(incoming.quotedAt());
        if (quote == null) {
            asset.updateCatalog(incoming.name());
            quote = quotes.save(new Quote(asset, incoming.price(), incoming.currency(), quotedAt,
                    atUtc(incoming.collectedAt()), incoming.source()));
        } else if (quotedAt.isAfter(quote.getQuotedAt())) {
            asset.updateCatalog(incoming.name());
            quote.replace(incoming.price(), incoming.currency(), quotedAt,
                    atUtc(incoming.collectedAt()), incoming.source());
        } else if (quotedAt.equals(quote.getQuotedAt())) {
            quote.clearStale();
        }
        return view(asset, quote, freshness);
    }

    @Transactional(readOnly = true)
    public CachedAssetQuote find(String ticker, com.projeto.gestao.domain.model.Market market,
            MarketDataFreshness freshness) {
        Asset asset = assets.findByTickerIgnoreCaseAndMarket(ticker, market).orElse(null);
        if (asset == null || asset.getStatus() != com.projeto.gestao.domain.model.AssetStatus.ACTIVE) return null;
        Quote quote = quotes.findById(asset.getId()).orElse(null);
        return quote == null ? null : view(asset, quote, freshness);
    }

    @Transactional
    public CachedExchangeRate store(UsdBrlRate incoming, MarketDataFreshness freshness) {
        validate(incoming);
        ExchangeRate rate = rates.findByCurrencyPair(USD_BRL).orElse(null);
        OffsetDateTime quotedAt = atUtc(incoming.quotedAt());
        if (rate == null) {
            rate = rates.save(new ExchangeRate(USD_BRL, incoming.rate(), quotedAt,
                    atUtc(incoming.collectedAt()), incoming.source()));
        } else if (quotedAt.isAfter(rate.getQuotedAt())) {
            rate.replace(incoming.rate(), quotedAt, atUtc(incoming.collectedAt()), incoming.source());
        } else if (quotedAt.equals(rate.getQuotedAt())) {
            rate.clearStale();
        }
        return view(rate, freshness);
    }

    @Transactional(readOnly = true)
    public CachedExchangeRate findExchangeRate(MarketDataFreshness freshness) {
        return rates.findById(USD_BRL).map(rate -> view(rate, freshness)).orElse(null);
    }

    @Transactional
    public void markQuoteStale(java.util.UUID assetId) {
        quotes.findById(assetId).ifPresent(Quote::markStale);
    }

    @Transactional
    public void markExchangeRateStale() {
        rates.findById(USD_BRL).ifPresent(ExchangeRate::markStale);
    }

    private static CachedAssetQuote view(Asset asset, Quote quote, MarketDataFreshness freshness) {
        return new CachedAssetQuote(asset.getId(), asset.getTicker(), asset.getName(), asset.getMarket(),
                quote.getCurrency(), quote.getPrice(), quote.getSource(),
                quote.getQuotedAt().toInstant(), quote.getCollectedAt().toInstant(),
                quote.isStale() || freshness.quoteIsStale(quote.getQuotedAt().toInstant()));
    }

    private static CachedExchangeRate view(ExchangeRate rate, MarketDataFreshness freshness) {
        return new CachedExchangeRate(rate.getRate(), rate.getSource(), rate.getQuotedAt().toInstant(),
                rate.getCollectedAt().toInstant(),
                rate.isStale() || freshness.exchangeRateIsStale(rate.getQuotedAt().toInstant()));
    }

    private static OffsetDateTime atUtc(java.time.Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }

    private static void validate(MarketQuote quote) {
        if (quote == null || quote.ticker() == null || quote.ticker().isBlank()
                || quote.name() == null || quote.name().isBlank() || quote.market() == null
                || quote.currency() == null || quote.price() == null || quote.price().signum() <= 0
                || quote.quotedAt() == null || quote.collectedAt() == null
                || quote.source() == null || quote.source().isBlank()) {
            throw new IllegalArgumentException("Invalid market quote snapshot");
        }
        if ((quote.market() == com.projeto.gestao.domain.model.Market.BR
                && quote.currency() != com.projeto.gestao.domain.model.Currency.BRL)
                || (quote.market() == com.projeto.gestao.domain.model.Market.US
                && quote.currency() != com.projeto.gestao.domain.model.Currency.USD)) {
            throw new IllegalArgumentException("Market and currency do not match");
        }
    }

    private static void validate(UsdBrlRate rate) {
        if (rate == null || rate.rate() == null || rate.rate().signum() <= 0
                || rate.quotedAt() == null || rate.collectedAt() == null
                || rate.source() == null || rate.source().isBlank()) {
            throw new IllegalArgumentException("Invalid exchange-rate snapshot");
        }
    }
}
