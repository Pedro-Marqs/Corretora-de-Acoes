package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MarketDataFreshnessTests {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private final MarketDataFreshness freshness =
            new MarketDataFreshness(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test void quoteBecomesStaleOnlyAfterTwentyFourHours() {
        assertThat(freshness.quoteIsStale(NOW.minusSeconds(24 * 60 * 60))).isFalse();
        assertThat(freshness.quoteIsStale(NOW.minusSeconds(24 * 60 * 60 + 1))).isTrue();
    }

    @Test void exchangeRateBecomesStaleOnlyAfterSevenDays() {
        assertThat(freshness.exchangeRateIsStale(NOW.minusSeconds(7 * 24 * 60 * 60))).isFalse();
        assertThat(freshness.exchangeRateIsStale(NOW.minusSeconds(7 * 24 * 60 * 60 + 1))).isTrue();
    }
}
