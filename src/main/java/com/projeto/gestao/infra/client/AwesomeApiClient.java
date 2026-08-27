package com.projeto.gestao.infra.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.config.ExternalIntegrationProperties;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.dto.AwesomeApiQuoteResponse;

@Component
public class AwesomeApiClient {
    public static final String SOURCE = "AwesomeAPI";

    private final URI baseUrl;
    private final String apiKey;
    private final Duration readTimeout;
    private final ExternalHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public AwesomeApiClient(ExternalIntegrationProperties properties, ObjectMapper objectMapper) {
        this(properties.awesomeApi().baseUrl(), properties.awesomeApi().apiKey(),
                properties.awesomeApi().readTimeout(),
                new JdkExternalHttpTransport(properties.awesomeApi().connectTimeout(), SOURCE), objectMapper);
    }

    public AwesomeApiClient(URI baseUrl, String apiKey, Duration readTimeout,
            ExternalHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.readTimeout = readTimeout;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public AwesomeApiQuoteResponse currentUsdBrl() {
        URI uri = URI.create(baseUrl.toString().replaceAll("/+$", "") + "/json/last/USD-BRL");
        Map<String, String> headers = apiKey.isBlank()
                ? Map.of()
                : Map.of("Authorization", "Bearer " + apiKey);
        ExternalHttpResponse response = transport.get(uri, readTimeout, headers);
        ExternalHttpStatusMapper.requireSuccess(response.statusCode(), SOURCE);
        try {
            AwesomeApiQuoteResponse parsed = objectMapper.readValue(response.body(), AwesomeApiQuoteResponse.class);
            if (parsed == null) {
                throw invalidResponse(null);
            }
            return parsed;
        } catch (IOException exception) {
            throw invalidResponse(exception);
        }
    }

    private static ExternalDataFailure invalidResponse(Throwable cause) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_RESPONSE, SOURCE,
                "Conteúdo inválido recebido da AwesomeAPI", cause);
    }
}
