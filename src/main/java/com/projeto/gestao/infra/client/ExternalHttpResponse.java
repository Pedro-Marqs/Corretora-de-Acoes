package com.projeto.gestao.infra.client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record ExternalHttpResponse(int statusCode, byte[] body) {

    public ExternalHttpResponse {
        body = Arrays.copyOf(body, body.length);
    }

    @Override
    public byte[] body() {
        return Arrays.copyOf(body, body.length);
    }

    public String bodyAsText() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
