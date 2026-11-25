package com.projeto.gestao.infra.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExternalHttpResponseTests {

    @Test
    void normalizesHeadersAndIgnoresNullEntries() {
        Map<String, String> headers = new HashMap<>();
        headers.put(null, "ignored");
        headers.put("X-Trace", null);
        headers.put("Retry-After", "2");

        ExternalHttpResponse response = new ExternalHttpResponse(429,
                "body".getBytes(StandardCharsets.UTF_8), headers);

        assertThat(response.header("retry-after")).isEqualTo("2");
        assertThat(response.header("X-Trace")).isNull();
        assertThat(response.header(null)).isNull();
        assertThat(response.bodyAsText()).isEqualTo("body");
    }

    @Test
    void acceptsNullHeaderMapAsEmpty() {
        ExternalHttpResponse response = new ExternalHttpResponse(200,
                "body".getBytes(StandardCharsets.UTF_8), null);

        assertThat(response.header("Retry-After")).isNull();
    }
}
