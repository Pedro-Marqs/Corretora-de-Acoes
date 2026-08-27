package com.projeto.gestao.infra.client.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class MarketDataProbeTests {
    @Test
    void mapsBrapiStockAndPrefersLongName() throws IOException {
        var quote = MarketDataProbe.mapBrapi(200, fixture("brapi-petr4.json"));

        assertThat(quote.symbol()).isEqualTo("PETR4");
        assertThat(quote.name()).isEqualTo("Petróleo Brasileiro S.A. - Petrobras");
        assertThat(quote.market()).isEqualTo("BR");
        assertThat(quote.currency()).isEqualTo("BRL");
        assertThat(quote.price()).isEqualByComparingTo("38.50");
        assertThat(quote.marketTime()).isEqualTo(Instant.parse("2026-06-14T17:08:00Z"));
    }

    @Test
    void rejectsMissingBrapiResultAndIncompleteSnapshot() throws IOException {
        assertFailure(() -> MarketDataProbe.mapBrapi(200, fixture("brapi-not-found.json")),
                MarketDataProbe.Kind.NOT_FOUND);
        assertFailure(() -> MarketDataProbe.mapBrapi(200, fixture("brapi-incomplete.json")),
                MarketDataProbe.Kind.INCOMPLETE_RESPONSE);
        assertFailure(() -> MarketDataProbe.mapBrapi(200, fixture("brapi-petr4.json"), "US"),
                MarketDataProbe.Kind.UNSUPPORTED_MARKET);
    }

    @Test
    void mapsRepresentativeUsExchangesWithoutInferringMarketFromCurrency() throws IOException {
        var nasdaq = MarketDataProbe.mapTwelveData(200, fixture("twelve-aapl.json"));
        var nyse = MarketDataProbe.mapTwelveData(200, fixture("twelve-ko.json"));

        assertThat(nasdaq.market()).isEqualTo("US");
        assertThat(nasdaq.name()).isEqualTo("Apple Inc");
        assertThat(nasdaq.price()).isEqualByComparingTo("226.34");
        assertThat(nyse.market()).isEqualTo("US");
        assertThat(nyse.symbol()).isEqualTo("KO");
        assertFailure(() -> MarketDataProbe.mapTwelveData(200, fixture("twelve-shop-tsx.json")),
                MarketDataProbe.Kind.UNSUPPORTED_MARKET);
    }

    @Test
    void handlesTwelveDataErrorsAndIncompleteSnapshots() throws IOException {
        assertFailure(() -> MarketDataProbe.mapTwelveData(200, fixture("twelve-not-found.json")),
                MarketDataProbe.Kind.NOT_FOUND);
        assertFailure(() -> MarketDataProbe.mapTwelveData(200, fixture("twelve-incomplete.json")),
                MarketDataProbe.Kind.INCOMPLETE_RESPONSE);
        assertFailure(() -> MarketDataProbe.mapTwelveData(200, fixture("twelve-auth-error.json")),
                MarketDataProbe.Kind.AUTHENTICATION);
        assertFailure(() -> MarketDataProbe.mapTwelveData(200, fixture("twelve-rate-limit.json")),
                MarketDataProbe.Kind.RATE_LIMITED);
    }

    @Test
    void mapsAwesomeApiUsdBrlWithBothSidesAndNumericTimestamp() throws IOException {
        var quote = MarketDataProbe.mapAwesomeApi(200, fixture("awesome-usd-brl.json"));

        assertThat(quote.base()).isEqualTo("USD");
        assertThat(quote.quote()).isEqualTo("BRL");
        assertThat(quote.bid()).isEqualByComparingTo("5.4321");
        assertThat(quote.ask()).isEqualByComparingTo("5.4331");
        assertThat(quote.marketTime()).isEqualTo(Instant.ofEpochSecond(1787857200));
        assertThat(quote.localMarketTime()).isEqualTo("2026-08-27 12:00:00");
    }

    @Test
    void rejectsInvalidAndIncompleteAwesomeApiResponses() throws IOException {
        assertFailure(() -> MarketDataProbe.mapAwesomeApi(404, fixture("awesome-not-found.json")),
                MarketDataProbe.Kind.NOT_FOUND);
        assertFailure(() -> MarketDataProbe.mapAwesomeApi(200, fixture("awesome-incomplete.json")),
                MarketDataProbe.Kind.INCOMPLETE_RESPONSE);
        assertFailure(() -> MarketDataProbe.mapAwesomeApi(200, fixture("awesome-wrong-pair.json")),
                MarketDataProbe.Kind.UNSUPPORTED_MARKET);
    }

    @Test
    void distinguishesControlledProviderFailures() {
        assertFailure(() -> MarketDataProbe.mapBrapi(429, "{}"), MarketDataProbe.Kind.RATE_LIMITED);
        assertFailure(() -> MarketDataProbe.mapTwelveData(503, "{}"), MarketDataProbe.Kind.UNAVAILABLE);
        assertFailure(() -> MarketDataProbe.mapAwesomeApi(200, "not-json"),
                MarketDataProbe.Kind.INCOMPLETE_RESPONSE);
    }

    @Test
    void derivesBrapiIntervalFromRefreshQuotaBatchingAndSafetyMargin() {
        var freeTen = MarketDataProbe.sustainableBrapiPolicy(10, 1, 15_000, 600, 22, 30, 80);
        var freeFifty = MarketDataProbe.sustainableBrapiPolicy(50, 1, 15_000, 600, 22, 30, 80);
        var startupFifty = MarketDataProbe.sustainableBrapiPolicy(50, 10, 150_000, 600, 22, 15, 80);

        assertThat(freeTen.intervalMinutes()).isEqualTo(30);
        assertThat(freeTen.projectedMonthlyRequests()).isEqualTo(4_400);
        assertThat(freeFifty.intervalMinutes()).isGreaterThanOrEqualTo(60);
        assertThat(freeFifty.projectedMonthlyRequests()).isLessThanOrEqualTo(freeFifty.safeMonthlyBudget());
        assertThat(startupFifty.intervalMinutes()).isEqualTo(15);
        assertThat(startupFifty.projectedMonthlyRequests()).isEqualTo(4_400);
        assertThatThrownBy(() -> MarketDataProbe.sustainableBrapiPolicy(
                1_000, 1, 100, 600, 22, 30, 80))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("one cycle");
    }

    @Test
    void calculatesTwelveDataDailyCreditConsumptionPerUniqueSymbol() {
        assertThat(MarketDataProbe.twelveDataDailyCredits(10, 1)).isEqualTo(10);
        assertThat(MarketDataProbe.twelveDataDailyCredits(100, 1)).isEqualTo(100);
        assertThat(MarketDataProbe.twelveDataDailyCredits(500, 1)).isEqualTo(500);
    }

    private static String fixture(String name) throws IOException {
        try (var stream = MarketDataProbeTests.class.getResourceAsStream("/t21/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertFailure(ThrowingRunnable action, MarketDataProbe.Kind kind) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(MarketDataProbe.ProbeFailure.class,
                        failure -> assertThat(failure.kind()).isEqualTo(kind));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
