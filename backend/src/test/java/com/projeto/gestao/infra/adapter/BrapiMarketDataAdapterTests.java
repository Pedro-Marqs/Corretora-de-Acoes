package com.projeto.gestao.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.BrapiClient;
import com.projeto.gestao.infra.client.ExternalHttpResponse;
import com.projeto.gestao.infra.client.ExternalHttpTransport;

class BrapiMarketDataAdapterTests {
    private static final Instant COLLECTED_AT = Instant.parse("2026-08-27T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(COLLECTED_AT, ZoneOffset.UTC);

    @Test
    void mapsValidFixtureNormalizesTickerAndRoundsHalfUp() {
        AtomicInteger calls = new AtomicInteger();
        var adapter = adapter(200, valid("38.505", "Petrobras", "PETROBRAS PN", "BRL",
                "2026-08-27T14:52:30Z"), calls);

        var quote = adapter.findQuote(" petr4 ");

        assertThat(quote.ticker()).isEqualTo("PETR4");
        assertThat(quote.name()).isEqualTo("Petrobras");
        assertThat(quote.market()).isEqualTo(Market.BR);
        assertThat(quote.currency()).isEqualTo(Currency.BRL);
        assertThat(quote.price()).isEqualByComparingTo("38.51");
        assertThat(quote.quotedAt()).isEqualTo(Instant.parse("2026-08-27T14:52:30Z"));
        assertThat(quote.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(quote.source()).isEqualTo(BrapiClient.SOURCE);
        assertThat(calls).hasValue(1);
    }

    @Test
    void fallsBackToShortName() {
        var quote = adapter(200, valid("38.50", null, "PETROBRAS PN", "BRL",
                "2026-08-27T14:52:30Z"), new AtomicInteger()).findQuote("PETR4");

        assertThat(quote.name()).isEqualTo("PETROBRAS PN");
    }

    @Test
    void rejectsNotFoundIncompleteCurrencyPriceAndTimestamp() {
        assertReason(() -> adapter(200, "{\"results\":[]}", new AtomicInteger()).findQuote("PETR4"),
                ExternalDataFailure.Reason.NOT_FOUND);
        assertReason(() -> adapter(200, valid("38.50", null, null, "BRL",
                "2026-08-27T14:52:30Z"), new AtomicInteger()).findQuote("PETR4"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, valid("38.50", "Petrobras", null, "USD",
                "2026-08-27T14:52:30Z"), new AtomicInteger()).findQuote("PETR4"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, valid("0", "Petrobras", null, "BRL",
                "2026-08-27T14:52:30Z"), new AtomicInteger()).findQuote("PETR4"),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, valid("38.50", "Petrobras", null, "BRL", null),
                new AtomicInteger()).findQuote("PETR4"), ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
    }

    @Test
    void mapsHttpAndInvalidJsonFailuresWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();
        assertReason(() -> adapter(429, "{}", calls).findQuote("PETR4"),
                ExternalDataFailure.Reason.RATE_LIMITED);
        assertThat(calls).hasValue(1);
        calls.set(0);
        assertReason(() -> adapter(503, "{}", calls).findQuote("PETR4"),
                ExternalDataFailure.Reason.SERVER_ERROR);
        assertThat(calls).hasValue(1);
        assertReason(() -> adapter(200, "not-json", new AtomicInteger()).findQuote("PETR4"),
                ExternalDataFailure.Reason.INVALID_RESPONSE);
    }

    @Test
    void propagatesTimeoutAndTransportFailureWithoutRetry() {
        AtomicInteger timeoutCalls = new AtomicInteger();
        BrapiClient timeoutClient = client((uri, timeout) -> {
            timeoutCalls.incrementAndGet();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TIMEOUT, BrapiClient.SOURCE,
                    "Tempo limite excedido", new HttpTimeoutException("timeout"));
        });
        assertReason(() -> new BrapiMarketDataAdapter(timeoutClient, CLOCK).findQuote("PETR4"),
                ExternalDataFailure.Reason.TIMEOUT);
        assertThat(timeoutCalls).hasValue(1);

        AtomicInteger transportCalls = new AtomicInteger();
        BrapiClient transportClient = client((uri, timeout) -> {
            transportCalls.incrementAndGet();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TRANSPORT_ERROR, BrapiClient.SOURCE,
                    "Falha de transporte");
        });
        assertReason(() -> new BrapiMarketDataAdapter(transportClient, CLOCK).findQuote("PETR4"),
                ExternalDataFailure.Reason.TRANSPORT_ERROR);
        assertThat(transportCalls).hasValue(1);
    }

    @Test
    void sendsBearerHeaderWithoutExposingCredentialInFailures() {
        CapturingTransport transport = new CapturingTransport(new ExternalHttpResponse(401, bytes("{}")));
        String secret = "local-secret-token";
        BrapiClient client = new BrapiClient(URI.create("https://brapi.test/api"), secret,
                Duration.ofSeconds(1), transport, json());

        assertThatThrownBy(() -> client.findQuote("PETR4"))
                .isInstanceOfSatisfying(ExternalDataFailure.class, failure -> {
                    assertThat(failure.getMessage()).doesNotContain(secret);
                    assertThat(failure.source()).isEqualTo(BrapiClient.SOURCE);
                });
        assertThat(transport.headers()).containsEntry("Authorization", "Bearer " + secret);
        assertThat(transport.uri().toString()).doesNotContain(secret);
    }

    private static BrapiMarketDataAdapter adapter(int status, String body, AtomicInteger calls) {
        return new BrapiMarketDataAdapter(client((uri, timeout) -> {
            calls.incrementAndGet();
            return new ExternalHttpResponse(status, bytes(body));
        }), CLOCK);
    }

    private static BrapiClient client(ExternalHttpTransport transport) {
        return new BrapiClient(URI.create("https://brapi.test/api"), "", Duration.ofSeconds(1),
                transport, json());
    }

    private static ObjectMapper json() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String valid(String price, String longName, String shortName, String currency,
            String timestamp) {
        return """
                {"results":[{"requestedSymbol":"PETR4","symbol":"PETR4","data":{
                  "shortName":%s,"longName":%s,"currency":%s,
                  "regularMarketPrice":%s,"regularMarketTime":%s}}]}
                """.formatted(jsonString(shortName), jsonString(longName), jsonString(currency), price,
                        jsonString(timestamp));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static void assertReason(Runnable action, ExternalDataFailure.Reason reason) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ExternalDataFailure.class,
                failure -> assertThat(failure.reason()).isEqualTo(reason));
    }

    private static final class CapturingTransport implements ExternalHttpTransport {
        private final ExternalHttpResponse response;
        private URI uri;
        private Map<String, String> headers = Map.of();

        private CapturingTransport(ExternalHttpResponse response) {
            this.response = response;
        }

        @Override
        public ExternalHttpResponse get(URI requestUri, Duration timeout) {
            return response;
        }

        @Override
        public ExternalHttpResponse get(URI requestUri, Duration timeout, Map<String, String> requestHeaders) {
            uri = requestUri;
            headers = Map.copyOf(requestHeaders);
            return response;
        }

        URI uri() {
            return uri;
        }

        Map<String, String> headers() {
            return headers;
        }
    }
}
