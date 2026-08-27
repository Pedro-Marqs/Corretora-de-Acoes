package com.projeto.gestao.infra.client;

import java.net.URI;
import java.time.Duration;

@FunctionalInterface
public interface ExternalHttpTransport {
    ExternalHttpResponse get(URI uri, Duration timeout);
}
