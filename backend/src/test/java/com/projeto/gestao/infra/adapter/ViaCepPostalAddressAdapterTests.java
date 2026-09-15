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
import com.projeto.gestao.infra.client.ExternalHttpResponse;
import com.projeto.gestao.infra.client.ExternalHttpTransport;
import com.projeto.gestao.infra.client.ViaCepClient;

class ViaCepPostalAddressAdapterTests {

    @Test
    void mapsValidResponseToInternalModel() {
        var adapter = adapter(response(200, """
                {"cep":"22250-911","logradouro":"Praia Botafogo","complemento":"",
                 "bairro":"Botafogo","localidade":"Rio de Janeiro","uf":"RJ"}
                """));

        var address = adapter.findByPostalCode("22250-911");

        assertThat(address.postalCode()).isEqualTo("22250911");
        assertThat(address.street()).isEqualTo("Praia Botafogo");
        assertThat(address.city()).isEqualTo("Rio de Janeiro");
    }

    @Test
    void distinguishesNotFoundIncompleteAndInvalidContent() {
        assertReason(adapter(response(400, "{}")), ExternalDataFailure.Reason.INVALID_INPUT);
        assertReason(adapter(response(200, "{\"erro\":true}")), ExternalDataFailure.Reason.NOT_FOUND);
        assertReason(adapter(response(200, "{\"cep\":\"22250-911\"}")),
                ExternalDataFailure.Reason.INCOMPLETE_RESPONSE);
        assertReason(adapter(response(200, "")), ExternalDataFailure.Reason.INVALID_RESPONSE);
        assertReason(adapter(response(404, "{}")), ExternalDataFailure.Reason.NOT_FOUND);
    }

    @Test
    void distinguishesRateLimitServerTimeoutAndTransportWithoutRetry() {
        AtomicInteger calls = new AtomicInteger();
        assertReason(adapter((uri, timeout) -> {
            calls.incrementAndGet();
            return value(429, "{}");
        }), ExternalDataFailure.Reason.RATE_LIMITED);
        assertThat(calls).hasValue(1);
        assertReason(adapter(response(500, "{}")), ExternalDataFailure.Reason.SERVER_ERROR);
        assertReason(adapter((uri, timeout) -> {
            throw failure(ExternalDataFailure.Reason.TIMEOUT);
        }), ExternalDataFailure.Reason.TIMEOUT);
        assertReason(adapter((uri, timeout) -> {
            throw failure(ExternalDataFailure.Reason.TRANSPORT_ERROR);
        }), ExternalDataFailure.Reason.TRANSPORT_ERROR);
    }

    private static ViaCepPostalAddressAdapter adapter(ExternalHttpTransport transport) {
        var client = new ViaCepClient(URI.create("https://example.test/ws"), Duration.ofSeconds(1),
                transport, new ObjectMapper());
        return new ViaCepPostalAddressAdapter(client);
    }

    private static ExternalHttpTransport response(int status, String body) {
        ExternalHttpResponse response = value(status, body);
        return (uri, timeout) -> response;
    }

    private static ExternalHttpResponse value(int status, String body) {
        return new ExternalHttpResponse(status, body.getBytes(StandardCharsets.UTF_8));
    }

    private static ExternalDataFailure failure(ExternalDataFailure.Reason reason) {
        return new ExternalDataFailure(reason, ViaCepClient.SOURCE, "simulated");
    }

    private static void assertReason(ViaCepPostalAddressAdapter adapter, ExternalDataFailure.Reason reason) {
        assertThatThrownBy(() -> adapter.findByPostalCode("22250911"))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.reason()).isEqualTo(reason));
    }
}
