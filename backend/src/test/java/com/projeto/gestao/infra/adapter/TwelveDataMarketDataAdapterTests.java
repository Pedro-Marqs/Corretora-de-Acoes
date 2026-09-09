package com.projeto.gestao.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.ExternalHttpResponse;
import com.projeto.gestao.infra.client.ExternalHttpTransport;
import com.projeto.gestao.infra.client.TwelveDataClient;

class TwelveDataMarketDataAdapterTests {
    private static final Instant COLLECTED_AT = Instant.parse("2026-08-27T16:00:00Z");
    private static final Clock CLOCK = Clock.fixed(COLLECTED_AT, ZoneOffset.UTC);

    @Test
    void mapsValidatedNasdaqAndNyseFixturesAndRoundsHalfUp() {
        var apple = adapter(200, quote("AAPL", "Apple Inc.", "NASDAQ", "XNGS", "USD",
                "314.585", 1787837400L), new AtomicInteger()).findQuote(" aapl ");
        var cocaCola = adapter(200, quote("KO", "The Coca-Cola Company", "NYSE", "XNYS", "USD",
                "69.124", 1787837400L), new AtomicInteger()).findQuote("ko");

        assertThat(apple.ticker()).isEqualTo("AAPL");
        assertThat(apple.name()).isEqualTo("Apple Inc.");
        assertThat(apple.market()).isEqualTo(Market.US);
        assertThat(apple.currency()).isEqualTo(Currency.USD);
        assertThat(apple.price()).isEqualByComparingTo("314.59");
        assertThat(apple.quotedAt()).isEqualTo(Instant.ofEpochSecond(1787837400L));
        assertThat(apple.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(apple.source()).isEqualTo(TwelveDataClient.SOURCE);
        assertThat(cocaCola.price()).isEqualByComparingTo("69.12");
    }

    @Test
    void rejectsExchangeOrMicOutsideValidatedUsAllowlist() {
        assertReason(() -> adapter(200, quote("SHOP", "Shopify Inc.", "TSX", "XTSE", "CAD",
                "150.10", 1787837400L), new AtomicInteger()).findQuote("SHOP"),
                ExternalDataFailure.Reason.INVALID_INPUT);
        assertReason(() -> adapter(200, quote("AAPL", "Apple Inc.", "NASDAQ", "XNYS", "USD",
                "314.58", 1787837400L), new AtomicInteger()).findQuote("AAPL"),
                ExternalDataFailure.Reason.INVALID_INPUT);
    }

    @Test
    void acceptsAllDocumentedUsMicAndExchangeCombinations() {
        assertAccepted("XNAS", "NASDAQ");
        assertAccepted("XNGS", "NASDAQ");
        assertAccepted("XNMS", "NASDAQ");
        assertAccepted("XNCM", "NASDAQ");
        assertAccepted("XNYS", "NYSE");
        assertAccepted("XASE", "NYSE AMERICAN");
        assertAccepted("ARCX", "NYSE ARCA");
        assertAccepted("BATS", "CBOE BZX");
    }

    @Test
    void rejectsIncompleteCurrencyPriceAndTimestamp() {
        assertReason(() -> adapter(200, quote("AAPL", null, "NASDAQ", "XNGS", "USD",
                "314.58", 1787837400L), new AtomicInteger()).findQuote("AAPL"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, quote("AAPL", "Apple", "NASDAQ", "XNGS", "CAD",
                "314.58", 1787837400L), new AtomicInteger()).findQuote("AAPL"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, quote("AAPL", "Apple", "NASDAQ", "XNGS", "USD",
                "0", 1787837400L), new AtomicInteger()).findQuote("AAPL"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, quote("AAPL", "Apple", "NASDAQ", "XNGS", "USD",
                "314.58", null), new AtomicInteger()).findQuote("AAPL"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
    }

    @Test
    void interpretsStructuredAuthenticationNotFoundRateLimitAndServerErrors() {
        assertStructured(401, ExternalDataFailure.Reason.AUTHENTICATION);
        assertStructured(404, ExternalDataFailure.Reason.NOT_FOUND);
        assertStructured(429, ExternalDataFailure.Reason.RATE_LIMITED);
        assertStructured(503, ExternalDataFailure.Reason.SERVER_ERROR);
    }

    @Test
    void mapsHttpInvalidJsonTimeoutAndTransportWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();
        assertReason(() -> adapter(429, "{}", calls).findQuote("AAPL"),
                ExternalDataFailure.Reason.RATE_LIMITED);
        assertThat(calls).hasValue(1);
        calls.set(0);
        assertReason(() -> adapter(502, "{}", calls).findQuote("AAPL"),
                ExternalDataFailure.Reason.SERVER_ERROR);
        assertThat(calls).hasValue(1);
        assertReason(() -> adapter(200, "not-json", new AtomicInteger()).findQuote("AAPL"),
                ExternalDataFailure.Reason.INVALID_RESPONSE);

        AtomicInteger timeoutCalls = new AtomicInteger();
        assertReason(() -> new TwelveDataMarketDataAdapter(client((uri, timeout) -> {
            timeoutCalls.incrementAndGet();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TIMEOUT, TwelveDataClient.SOURCE,
                    "Tempo limite excedido");
        }, "local-key"), CLOCK).findQuote("AAPL"), ExternalDataFailure.Reason.TIMEOUT);
        assertThat(timeoutCalls).hasValue(1);

        AtomicInteger transportCalls = new AtomicInteger();
        assertReason(() -> new TwelveDataMarketDataAdapter(client((uri, timeout) -> {
            transportCalls.incrementAndGet();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TRANSPORT_ERROR, TwelveDataClient.SOURCE,
                    "Falha de transporte");
        }, "local-key"), CLOCK).findQuote("AAPL"), ExternalDataFailure.Reason.TRANSPORT_ERROR);
        assertThat(transportCalls).hasValue(1);
    }

    @Test
    void requiresConfiguredApiKeyWithoutExposingItInFailures() {
        TwelveDataClient missing = client((uri, timeout) -> new ExternalHttpResponse(200, bytes("{}")), "");
        assertReason(() -> missing.findQuote("AAPL"), ExternalDataFailure.Reason.AUTHENTICATION);

        String secret = "local-twelve-secret";
        TwelveDataClient invalid = client((uri, timeout) -> new ExternalHttpResponse(401, bytes("{}")), secret);
        assertThatThrownBy(() -> invalid.findQuote("AAPL"))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.getMessage()).doesNotContain(secret));
    }

    private static void assertStructured(int code, ExternalDataFailure.Reason reason) {
        String body = "{\"code\":" + code + ",\"message\":\"provider detail\",\"status\":\"error\"}";
        assertReason(() -> adapter(200, body, new AtomicInteger()).findQuote("AAPL"), reason);
    }

    private static void assertAccepted(String mic, String exchange) {
        var result = adapter(200, quote("TEST", "Test Company", exchange, mic, "USD",
                "10.00", 1787837400L), new AtomicInteger()).findQuote("TEST");
        assertThat(result.market()).isEqualTo(Market.US);
    }

    private static TwelveDataMarketDataAdapter adapter(int status, String body, AtomicInteger calls) {
        return new TwelveDataMarketDataAdapter(client((uri, timeout) -> {
            calls.incrementAndGet();
            return new ExternalHttpResponse(status, bytes(body));
        }, "local-key"), CLOCK);
    }

    private static TwelveDataClient client(ExternalHttpTransport transport, String apiKey) {
        return new TwelveDataClient(URI.create("https://twelve.test"), apiKey, Duration.ofSeconds(1),
                transport, new ObjectMapper().findAndRegisterModules());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String quote(String symbol, String name, String exchange, String mic, String currency,
            String close, Long timestamp) {
        return """
                {"symbol":%s,"name":%s,"exchange":%s,"mic_code":%s,"currency":%s,
                 "close":%s,"timestamp":%s}
                """.formatted(json(symbol), json(name), json(exchange), json(mic), json(currency),
                        close, timestamp == null ? "null" : timestamp);
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static void assertReason(Runnable action, ExternalDataFailure.Reason reason) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ExternalDataFailure.class,
                failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
