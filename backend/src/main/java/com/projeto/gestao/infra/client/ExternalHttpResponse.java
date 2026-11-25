package com.projeto.gestao.infra.client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record ExternalHttpResponse(int statusCode, byte[] body, Map<String, String> headers) {

    public ExternalHttpResponse(int statusCode, byte[] body) {
        this(statusCode, body, Map.of());
    }

    public ExternalHttpResponse {
        Objects.requireNonNull(body, "body");
        body = Arrays.copyOf(body, body.length);
        Map<String, String> normalizedHeaders = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null) {
                    normalizedHeaders.put(name.toLowerCase(Locale.ROOT), value);
                }
            });
        }
        headers = Map.copyOf(normalizedHeaders);
    }

    @Override
    public byte[] body() {
        return Arrays.copyOf(body, body.length);
    }

    public String bodyAsText() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public String header(String name) {
        return name == null ? null : headers.get(name.toLowerCase(Locale.ROOT));
    }
}
