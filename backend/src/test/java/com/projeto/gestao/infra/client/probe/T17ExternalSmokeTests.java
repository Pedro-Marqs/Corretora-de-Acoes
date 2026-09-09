package com.projeto.gestao.infra.client.probe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("external")
class T17ExternalSmokeTests {
    private static final String XP = "02332886000104";
    private static final String MAGALU = "47960950000121";
    private static final String BRASIL_API = "https://brasilapi.com.br/api/cnpj/v1/";
    private static final String VIA_CEP = "https://viacep.com.br/ws/%s/json/";
    private static final String CVM_DATA = "https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip";
    private static final String CVM_META = "https://dados.cvm.gov.br/dados/INTERMED/CAD/META/meta_cad_intermed.zip";
    private static HttpClient http;

    @BeforeAll
    static void enableOnlyByExplicitOptIn() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("T17_EXTERNAL_SMOKE")),
                "Set T17_EXTERNAL_SMOKE=true to access the live public sources");
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Test
    void verifiesLivePositiveAndNegativeCasesAcrossOfficialSources() throws Exception {
        var xp = company(XP);
        var magalu = company(MAGALU);
        var xpAddress = address(xp.cep());
        var magaluAddress = address(magalu.cep());
        var cvm = CvmDatasetProbe.inspectDataZip(new ByteArrayInputStream(get(CVM_DATA).body()));
        String metadata = CvmDatasetProbe.inspectMetadataZip(
                new ByteArrayInputStream(get(CVM_META).body()));

        assertThat(xp.registrationStatus()).isEqualToIgnoringCase("ATIVA");
        assertThat(xpAddress.cep()).isEqualTo(xp.cep());
        assertThat(cvm.findByCnpj(XP)).anyMatch(CvmDatasetProbe.Participant::isActiveCtvm);
        assertThat(magalu.registrationStatus()).isEqualToIgnoringCase("ATIVA");
        assertThat(magaluAddress.cep()).isEqualTo(magalu.cep());
        assertThat(cvm.findByCnpj(MAGALU)).noneMatch(CvmDatasetProbe.Participant::isActiveCtvm);
        assertThat(metadata).contains("Campo: TP_PARTIC", "Campo: CNPJ", "Campo: SIT");
    }

    private static CnpjCepProbe.Company company(String cnpj) throws Exception {
        HttpResponse<byte[]> response = get(BRASIL_API + cnpj);
        return CnpjCepProbe.mapCompany(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
    }

    private static CnpjCepProbe.Address address(String cep) throws Exception {
        HttpResponse<byte[]> response = get(VIA_CEP.formatted(cep));
        return CnpjCepProbe.mapAddress(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
    }

    private static HttpResponse<byte[]> get(String url) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("External smoke request failed with HTTP " + response.statusCode());
        }
        return response;
    }
}
