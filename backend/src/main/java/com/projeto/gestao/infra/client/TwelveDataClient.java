package com.projeto.gestao.infra.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.config.ExternalIntegrationProperties;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.dto.TwelveDataQuoteResponse;

@Component
public class TwelveDataClient {
    public static final String SOURCE = "Twelve Data";

    private final URI baseUrl;
    private final String apiKey;
    private final Duration readTimeout;
    private final ExternalHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public TwelveDataClient(ExternalIntegrationProperties properties, ObjectMapper objectMapper) {
        this(properties.twelveData().baseUrl(), properties.twelveData().apiKey(),
                properties.twelveData().readTimeout(),
                new JdkExternalHttpTransport(properties.twelveData().connectTimeout(), SOURCE), objectMapper);
    }

    public TwelveDataClient(URI baseUrl, String apiKey, Duration readTimeout,
            ExternalHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.readTimeout = readTimeout;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public TwelveDataQuoteResponse findQuote(String ticker) {
        if (apiKey.isBlank()) {
            throw new ExternalDataFailure(ExternalDataFailure.Reason.AUTHENTICATION, SOURCE,
                    "Chave da Twelve Data não configurada");
        }
        URI uri = URI.create(baseUrl.toString().replaceAll("/+$", "") + "/quote?symbol="
                + encode(ticker) + "&apikey=" + encode(apiKey));
        ExternalHttpResponse response = transport.get(uri, readTimeout);
        ExternalHttpStatusMapper.requireSuccess(response.statusCode(), SOURCE);
        try {
            TwelveDataQuoteResponse parsed = objectMapper.readValue(response.body(), TwelveDataQuoteResponse.class);
            if (parsed == null) {
                throw invalidResponse(null);
            }
            return parsed;
        } catch (IOException exception) {
            throw invalidResponse(exception);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ExternalDataFailure invalidResponse(Throwable cause) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_RESPONSE, SOURCE,
                "Conteúdo inválido recebido da Twelve Data", cause);
    }
}
