package com.projeto.gestao.infra.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.projeto.gestao.domain.port.ExternalDataFailure;

public final class JdkExternalHttpTransport implements ExternalHttpTransport {

    private final HttpClient httpClient;
    private final String source;

    public JdkExternalHttpTransport(Duration connectTimeout, String source) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.source = source;
    }

    @Override
    public ExternalHttpResponse get(URI uri, Duration timeout) {
        return get(uri, timeout, Map.of());
    }

    @Override
    public ExternalHttpResponse get(URI uri, Duration timeout, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout).GET();
        headers.forEach(builder::header);
        HttpRequest request = builder.build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new ExternalHttpResponse(response.statusCode(), response.body());
        } catch (java.net.http.HttpTimeoutException exception) {
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TIMEOUT, source,
                    "Tempo limite excedido ao consultar " + source, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TRANSPORT_ERROR, source,
                    "Consulta a " + source + " interrompida", exception);
        } catch (IOException exception) {
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TRANSPORT_ERROR, source,
                    "Falha de transporte ao consultar " + source, exception);
        }
    }
}
