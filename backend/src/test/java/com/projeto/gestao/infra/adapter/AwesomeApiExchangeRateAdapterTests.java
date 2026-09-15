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
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.AwesomeApiClient;
import com.projeto.gestao.infra.client.ExternalHttpResponse;
import com.projeto.gestao.infra.client.ExternalHttpTransport;

class AwesomeApiExchangeRateAdapterTests {
    private static final Instant COLLECTED_AT = Instant.parse("2026-08-27T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(COLLECTED_AT, ZoneOffset.UTC);

    @Test
    void mapsBidOnlyRoundsHalfUpAndPreservesTimestamps() {
        var rate = adapter(200, valid("5.165", "99.99", "USD", "BRL", "1787843133"),
                new AtomicInteger()).currentRate();

        assertThat(rate.rate()).isEqualByComparingTo("5.17");
        assertThat(rate.rate()).isNotEqualByComparingTo("99.99");
        assertThat(rate.quotedAt()).isEqualTo(Instant.ofEpochSecond(1787843133));
        assertThat(rate.collectedAt()).isEqualTo(COLLECTED_AT);
        assertThat(rate.source()).isEqualTo(AwesomeApiClient.SOURCE);
    }

    @Test
    void rejectsWrongPairMissingAndInvalidRequiredFields() {
        assertReason(() -> adapter(200, valid("5.16", "5.17", "EUR", "BRL", "1787843133"),
                new AtomicInteger()).currentRate(), ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, "{}", new AtomicInteger()).currentRate(),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, valid("0", "5.17", "USD", "BRL", "1787843133"),
                new AtomicInteger()).currentRate(), ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(() -> adapter(200, valid("5.16", "5.17", "USD", "BRL", "0"),
                new AtomicInteger()).currentRate(), ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
    }

    @Test
    void mapsHttpInvalidJsonTimeoutAndTransportWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();
        assertReason(() -> adapter(429, "{}", calls).currentRate(), ExternalDataFailure.Reason.RATE_LIMITED);
        assertThat(calls).hasValue(1);
        calls.set(0);
        assertReason(() -> adapter(500, "{}", calls).currentRate(), ExternalDataFailure.Reason.SERVER_ERROR);
        assertThat(calls).hasValue(1);
        assertReason(() -> adapter(200, "invalid", new AtomicInteger()).currentRate(),
                ExternalDataFailure.Reason.INVALID_RESPONSE);

        AtomicInteger timeout = new AtomicInteger();
        assertReason(() -> new AwesomeApiExchangeRateAdapter(client((uri, duration) -> {
            timeout.incrementAndGet();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TIMEOUT, AwesomeApiClient.SOURCE,
                    "Tempo limite excedido");
        }), CLOCK).currentRate(), ExternalDataFailure.Reason.TIMEOUT);
        assertThat(timeout).hasValue(1);

        AtomicInteger transport = new AtomicInteger();
        assertReason(() -> new AwesomeApiExchangeRateAdapter(client((uri, duration) -> {
            transport.incrementAndGet();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TRANSPORT_ERROR, AwesomeApiClient.SOURCE,
                    "Falha de transporte");
        }), CLOCK).currentRate(), ExternalDataFailure.Reason.TRANSPORT_ERROR);
        assertThat(transport).hasValue(1);
    }

    @Test
    void credentialNeverAppearsInFailureMessageOrUrl() {
        String secret = "local-awesome-secret";
        AwesomeApiClient client = new AwesomeApiClient(URI.create("https://awesome.test"), secret,
                Duration.ofSeconds(1), (uri, timeout) -> new ExternalHttpResponse(401, bytes("{}")), json());

        assertThatThrownBy(client::currentUsdBrl).isInstanceOfSatisfying(ExternalDataFailure.class,
                failure -> assertThat(failure.getMessage()).doesNotContain(secret));
    }

    private static AwesomeApiExchangeRateAdapter adapter(int status, String body, AtomicInteger calls) {
        return new AwesomeApiExchangeRateAdapter(client((uri, timeout) -> {
            calls.incrementAndGet();
            return new ExternalHttpResponse(status, bytes(body));
        }), CLOCK);
    }

    private static AwesomeApiClient client(ExternalHttpTransport transport) {
        return new AwesomeApiClient(URI.create("https://awesome.test"), "", Duration.ofSeconds(1),
                transport, json());
    }

    private static ObjectMapper json() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String valid(String bid, String ask, String code, String codein, String timestamp) {
        return """
                {"USDBRL":{"code":"%s","codein":"%s","bid":"%s","ask":"%s","timestamp":"%s"}}
                """.formatted(code, codein, bid, ask, timestamp);
    }

    private static void assertReason(Runnable action, ExternalDataFailure.Reason reason) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ExternalDataFailure.class,
                failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
