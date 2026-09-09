package com.projeto.gestao.infra.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ExternalHttpTransportCompatibilityTests {
    @Test
    void optionalHeadersKeepExistingLambdaTransportsCompatible() {
        AtomicInteger calls = new AtomicInteger();
        ExternalHttpTransport transport = (uri, timeout) -> {
            calls.incrementAndGet();
            return new ExternalHttpResponse(200, new byte[0]);
        };

        ExternalHttpResponse response = transport.get(URI.create("https://example.test"),
                Duration.ofSeconds(1), Map.of("Authorization", "Bearer local-only"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(calls).hasValue(1);
    }
}
