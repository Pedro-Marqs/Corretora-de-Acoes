package com.projeto.gestao.infra.client.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CnpjCepProbeTests {
    @Test
    void mapsRepresentativeBrasilApiAndViaCepResponsesOffline() throws IOException {
        var company = CnpjCepProbe.mapCompany(200, fixture("brasilapi-xp.json"));
        var address = CnpjCepProbe.mapAddress(200, fixture("viacep-xp.json"));

        assertThat(company.cnpj()).isEqualTo("02332886000104");
        assertThat(company.legalName()).isEqualTo("XP INVESTIMENTOS CORRETORA DE CAMBIO, TITULOS E VALORES MOBILIARIOS S/A");
        assertThat(company.tradeName()).isEqualTo("XP INVESTIMENTOS CCTVM S/A");
        assertThat(company.registrationStatus()).isEqualTo("ATIVA");
        assertThat(company.cep()).isEqualTo("22250911");
        assertThat(address).isEqualTo(new CnpjCepProbe.Address(
                "22250911", "Praia Botafogo", "", "Botafogo", "Rio de Janeiro", "RJ"));
    }

    @Test
    void normalizesFormattedIdentifiersAndRejectsInvalidInputs() {
        assertThat(CnpjCepProbe.normalizeCnpj("02.332.886/0001-04")).isEqualTo("02332886000104");
        assertThat(CnpjCepProbe.normalizeCep("22440-032")).isEqualTo("22440032");

        assertFailure(() -> CnpjCepProbe.normalizeCnpj("123"), CnpjCepProbe.Kind.INVALID_INPUT);
        assertFailure(() -> CnpjCepProbe.normalizeCep("2244A-032"), CnpjCepProbe.Kind.INVALID_INPUT);
    }

    @Test
    void distinguishesInvalidNotFoundUnavailableAndIncompleteResponses() {
        assertFailure(() -> CnpjCepProbe.mapCompany(400, "{}"), CnpjCepProbe.Kind.INVALID_INPUT);
        assertFailure(() -> CnpjCepProbe.mapCompany(404, "{}"), CnpjCepProbe.Kind.NOT_FOUND);
        assertFailure(() -> CnpjCepProbe.mapCompany(503, "{}"), CnpjCepProbe.Kind.UNAVAILABLE);
        assertFailure(() -> CnpjCepProbe.mapCompany(200, "{\"cnpj\":\"02332886000104\"}"),
                CnpjCepProbe.Kind.INCOMPLETE_RESPONSE);
        assertFailure(() -> CnpjCepProbe.mapCompany(200, ""),
                CnpjCepProbe.Kind.INCOMPLETE_RESPONSE);
        assertFailure(() -> CnpjCepProbe.mapAddress(400, "{}"), CnpjCepProbe.Kind.INVALID_INPUT);
        assertFailure(() -> CnpjCepProbe.mapAddress(200, "{\"erro\":true}"), CnpjCepProbe.Kind.NOT_FOUND);
        assertFailure(() -> CnpjCepProbe.mapAddress(502, "{}"), CnpjCepProbe.Kind.UNAVAILABLE);
        assertFailure(() -> CnpjCepProbe.mapAddress(200, "not-json"),
                CnpjCepProbe.Kind.INCOMPLETE_RESPONSE);
        assertFailure(() -> CnpjCepProbe.mapAddress(200, ""),
                CnpjCepProbe.Kind.INCOMPLETE_RESPONSE);
    }

    private static String fixture(String name) throws IOException {
        try (var stream = CnpjCepProbeTests.class.getResourceAsStream("/t17/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertFailure(Runnable action, CnpjCepProbe.Kind kind) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(CnpjCepProbe.ProbeFailure.class,
                        failure -> assertThat(failure.kind()).isEqualTo(kind));
    }
}
