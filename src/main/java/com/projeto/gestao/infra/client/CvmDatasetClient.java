package com.projeto.gestao.infra.client;

import java.net.URI;
import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.projeto.gestao.config.ExternalIntegrationProperties;

@Component
public class CvmDatasetClient {

    public static final String SOURCE = "CVM";

    private final URI datasetUri;
    private final Duration readTimeout;
    private final ExternalHttpTransport transport;

    @Autowired
    public CvmDatasetClient(ExternalIntegrationProperties properties) {
        this(datasetUri(properties.cvm().baseUrl(), properties.cvm().datasetPath()),
                properties.cvm().readTimeout(),
                new JdkExternalHttpTransport(properties.cvm().connectTimeout(), SOURCE));
    }

    public CvmDatasetClient(URI datasetUri, Duration readTimeout, ExternalHttpTransport transport) {
        this.datasetUri = datasetUri;
        this.readTimeout = readTimeout;
        this.transport = transport;
    }

    public byte[] download() {
        ExternalHttpResponse response = transport.get(datasetUri, readTimeout);
        ExternalHttpStatusMapper.requireSuccess(response.statusCode(), SOURCE);
        return response.body();
    }

    private static URI datasetUri(URI baseUrl, String datasetPath) {
        return URI.create(baseUrl.toString().replaceAll("/+$", "") + "/"
                + datasetPath.replaceAll("^/+", ""));
    }
}
