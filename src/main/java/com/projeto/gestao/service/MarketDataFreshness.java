package com.projeto.gestao.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class MarketDataFreshness {
    public static final Duration QUOTE_MAX_AGE = Duration.ofHours(24);
    public static final Duration EXCHANGE_RATE_MAX_AGE = Duration.ofDays(7);
    private final Clock clock;

    public MarketDataFreshness(Clock clock) { this.clock = clock; }

    public boolean quoteIsStale(Instant quotedAt) {
        return quotedAt.isBefore(clock.instant().minus(QUOTE_MAX_AGE));
    }

    public boolean exchangeRateIsStale(Instant quotedAt) {
        return quotedAt.isBefore(clock.instant().minus(EXCHANGE_RATE_MAX_AGE));
    }
}
