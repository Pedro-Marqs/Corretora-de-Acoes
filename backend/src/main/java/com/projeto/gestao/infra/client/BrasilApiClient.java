package com.projeto.gestao.infra.client;

import java.net.URI;
import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.config.ExternalIntegrationProperties;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.dto.BrasilApiCompanyResponse;

@Component
public class BrasilApiClient {

    public static final String SOURCE = "BrasilAPI";

    private final URI baseUrl;
    private final Duration readTimeout;
    private final ExternalHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public BrasilApiClient(ExternalIntegrationProperties properties, ObjectMapper objectMapper) {
        this(properties.brasilApi().baseUrl(), properties.brasilApi().readTimeout(),
                new JdkExternalHttpTransport(properties.brasilApi().connectTimeout(), SOURCE), objectMapper);
    }

    public BrasilApiClient(URI baseUrl, Duration readTimeout, ExternalHttpTransport transport,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.readTimeout = readTimeout;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public BrasilApiCompanyResponse findByCnpj(String normalizedCnpj) {
        URI uri = endpoint(baseUrl, "/cnpj/v1/" + normalizedCnpj);
        ExternalHttpResponse response = transport.get(uri, readTimeout);
        ExternalHttpStatusMapper.requireSuccess(response.statusCode(), SOURCE);
        try {
            BrasilApiCompanyResponse parsed = objectMapper.readValue(response.body(), BrasilApiCompanyResponse.class);
            if (parsed == null) {
                throw invalidResponse(null);
            }
            return parsed;
        } catch (IOException exception) {
            throw invalidResponse(exception);
        }
    }

    private ExternalDataFailure invalidResponse(Throwable cause) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_RESPONSE, SOURCE,
                "Conteúdo inválido recebido da BrasilAPI", cause);
    }

    static URI endpoint(URI base, String path) {
        return URI.create(base.toString().replaceAll("/+$", "") + path);
    }
}
