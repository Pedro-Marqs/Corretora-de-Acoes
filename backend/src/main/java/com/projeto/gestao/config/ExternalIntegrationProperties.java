package com.projeto.gestao.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations")
public record ExternalIntegrationProperties(
        HttpSource brasilApi,
        HttpSource viaCep,
        CvmSource cvm,
        AuthenticatedHttpSource brapi,
        ApiKeyHttpSource twelveData,
        ApiKeyHttpSource awesomeApi) {

    public record HttpSource(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
    }

    public record AuthenticatedHttpSource(
            URI baseUrl, String token, Duration connectTimeout, Duration readTimeout) {
    }

    public record ApiKeyHttpSource(
            URI baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
    }

    public record CvmSource(
            URI baseUrl,
            String datasetPath,
            Duration connectTimeout,
            Duration readTimeout,
            Duration snapshotValidity) {
    }
}
