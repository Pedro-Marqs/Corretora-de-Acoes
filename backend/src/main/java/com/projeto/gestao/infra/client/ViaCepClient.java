package com.projeto.gestao.infra.client;

import java.net.URI;
import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.config.ExternalIntegrationProperties;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.dto.ViaCepAddressResponse;

@Component
public class ViaCepClient {

    public static final String SOURCE = "ViaCEP";

    private final URI baseUrl;
    private final Duration readTimeout;
    private final ExternalHttpTransport transport;
    private final ObjectMapper objectMapper;

    @Autowired
    public ViaCepClient(ExternalIntegrationProperties properties, ObjectMapper objectMapper) {
        this(properties.viaCep().baseUrl(), properties.viaCep().readTimeout(),
                new JdkExternalHttpTransport(properties.viaCep().connectTimeout(), SOURCE), objectMapper);
    }

    public ViaCepClient(URI baseUrl, Duration readTimeout, ExternalHttpTransport transport,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.readTimeout = readTimeout;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public ViaCepAddressResponse findByPostalCode(String normalizedPostalCode) {
        URI uri = BrasilApiClient.endpoint(baseUrl, "/" + normalizedPostalCode + "/json/");
        ExternalHttpResponse response = transport.get(uri, readTimeout);
        ExternalHttpStatusMapper.requireSuccess(response.statusCode(), SOURCE);
        try {
            ViaCepAddressResponse parsed = objectMapper.readValue(response.body(), ViaCepAddressResponse.class);
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
                "Conteúdo inválido recebido do ViaCEP", cause);
    }
}
