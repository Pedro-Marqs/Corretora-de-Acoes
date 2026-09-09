package com.projeto.gestao.infra.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.config.ExternalIntegrationProperties;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.dto.BrapiQuoteResponse;

@Component
public class BrapiClient {
    public static final String SOURCE = "Brapi";

    private final URI baseUrl;
    private final String token;
    private final Duration readTimeout;
    private final ExternalHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public BrapiClient(ExternalIntegrationProperties properties, ObjectMapper objectMapper) {
        this(properties.brapi().baseUrl(), properties.brapi().token(), properties.brapi().readTimeout(),
                new JdkExternalHttpTransport(properties.brapi().connectTimeout(), SOURCE), objectMapper);
    }

    public BrapiClient(URI baseUrl, String token, Duration readTimeout,
            ExternalHttpTransport transport, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.token = token == null ? "" : token;
        this.readTimeout = readTimeout;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public BrapiQuoteResponse findQuote(String ticker) {
        URI uri = endpoint(baseUrl, "/v2/stocks/quote?symbols=" + encode(ticker));
        Map<String, String> headers = token.isBlank()
                ? Map.of()
                : Map.of("Authorization", "Bearer " + token);
        ExternalHttpResponse response = transport.get(uri, readTimeout, headers);
        ExternalHttpStatusMapper.requireSuccess(response.statusCode(), SOURCE);
        try {
            BrapiQuoteResponse parsed = objectMapper.readValue(response.body(), BrapiQuoteResponse.class);
            if (parsed == null) {
                throw invalidResponse(null);
            }
            return parsed;
        } catch (IOException exception) {
            throw invalidResponse(exception);
        }
    }

    private static URI endpoint(URI base, String path) {
        return URI.create(base.toString().replaceAll("/+$", "") + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ExternalDataFailure invalidResponse(Throwable cause) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_RESPONSE, SOURCE,
                "Conteúdo inválido recebido da Brapi", cause);
    }
}
