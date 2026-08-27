package com.projeto.gestao.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.BrasilApiClient;
import com.projeto.gestao.infra.client.ExternalHttpResponse;
import com.projeto.gestao.infra.client.ExternalHttpTransport;

class BrasilApiCompanyRegistryAdapterTests {

    @Test
    void mapsValidResponseToInternalModel() {
        var adapter = adapter(response(200, """
                {"cnpj":"02.332.886/0001-04","razao_social":"XP INVESTIMENTOS S/A",
                 "nome_fantasia":"XP","descricao_situacao_cadastral":"ATIVA","cep":"22250-911"}
                """));

        var company = adapter.findByCnpj("02.332.886/0001-04");

        assertThat(company.cnpj()).isEqualTo("02332886000104");
        assertThat(company.postalCode()).isEqualTo("22250911");
        assertThat(company.legalName()).isEqualTo("XP INVESTIMENTOS S/A");
    }

    @Test
    void rejectsMissingRequiredFieldAndInvalidJson() {
        assertReason(adapter(response(200, "{\"cnpj\":\"02332886000104\"}")),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(adapter(response(200, "not-json")), ExternalDataFailure.Reason.INVALID_RESPONSE);
    }

    @Test
    void distinguishesNotFoundRateLimitAndServerErrorWithoutRetry() {
        assertReason(adapter(response(400, "{}")), ExternalDataFailure.Reason.INVALID_INPUT);
        assertReason(adapter(response(404, "{}")), ExternalDataFailure.Reason.NOT_FOUND);

        AtomicInteger calls = new AtomicInteger();
        ExternalHttpTransport limited = (uri, timeout) -> {
            calls.incrementAndGet();
            return responseValue(429, "{}");
        };
        assertReason(adapter(limited), ExternalDataFailure.Reason.RATE_LIMITED);
        assertThat(calls).hasValue(1);
        assertReason(adapter(response(503, "{}")), ExternalDataFailure.Reason.SERVER_ERROR);
    }

    @Test
    void preservesTimeoutAndTransportFailures() {
        assertReason(adapter((uri, timeout) -> {
            throw failure(ExternalDataFailure.Reason.TIMEOUT);
        }), ExternalDataFailure.Reason.TIMEOUT);
        assertReason(adapter((uri, timeout) -> {
            throw failure(ExternalDataFailure.Reason.TRANSPORT_ERROR);
        }), ExternalDataFailure.Reason.TRANSPORT_ERROR);
    }

    private static BrasilApiCompanyRegistryAdapter adapter(ExternalHttpTransport transport) {
        var client = new BrasilApiClient(URI.create("https://example.test/api"), Duration.ofSeconds(1),
                transport, new ObjectMapper());
        return new BrasilApiCompanyRegistryAdapter(client);
    }

    private static ExternalHttpTransport response(int status, String body) {
        ExternalHttpResponse response = responseValue(status, body);
        return (uri, timeout) -> response;
    }

    private static ExternalHttpResponse responseValue(int status, String body) {
        return new ExternalHttpResponse(status, body.getBytes(StandardCharsets.UTF_8));
    }

    private static ExternalDataFailure failure(ExternalDataFailure.Reason reason) {
        return new ExternalDataFailure(reason, BrasilApiClient.SOURCE, "simulated");
    }

    private static void assertReason(BrasilApiCompanyRegistryAdapter adapter,
            ExternalDataFailure.Reason reason) {
        assertThatThrownBy(() -> adapter.findByCnpj("02332886000104"))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
