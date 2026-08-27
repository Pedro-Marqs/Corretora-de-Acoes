package com.projeto.gestao.infra.client;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@FunctionalInterface
public interface ExternalHttpTransport {
    ExternalHttpResponse get(URI uri, Duration timeout);

    default ExternalHttpResponse get(URI uri, Duration timeout, Map<String, String> headers) {
        return get(uri, timeout);
    }
}
