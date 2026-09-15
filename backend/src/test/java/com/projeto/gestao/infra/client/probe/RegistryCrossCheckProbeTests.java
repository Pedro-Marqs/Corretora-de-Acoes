package com.projeto.gestao.infra.client.probe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegistryCrossCheckProbeTests {
    private static CvmDatasetProbe.Dataset cvm;

    @BeforeAll
    static void loadCvmFixture() throws IOException {
        var entries = new LinkedHashMap<String, String>();
        entries.put(CvmDatasetProbe.DATA_ENTRY, fixture("cvm-participants.csv"));
        entries.put(CvmDatasetProbe.RESPONSIBLES_ENTRY, "TP_PARTIC;CNPJ;DENOM_SOCIAL\n");
        cvm = CvmDatasetProbe.inspectDataZip(zip(entries));
    }

    @Test
    void acceptsActiveCompanyOnlyWhenOfficialCvmDatasetConfirmsCtvm() throws IOException {
        var company = CnpjCepProbe.mapCompany(200, fixture("brasilapi-xp.json"));
        var address = CnpjCepProbe.mapAddress(200, fixture("viacep-xp.json"));

        assertThat(company.registrationStatus()).isEqualTo("ATIVA");
        assertThat(address.cep()).isEqualTo(company.cep());
        assertThat(cvm.findByCnpj(company.cnpj()))
                .anyMatch(CvmDatasetProbe.Participant::isActiveCtvm);
    }

    @Test
    void rejectsActiveValidCompanyWhenCvmDoesNotClassifyItAsCtvm() throws IOException {
        var company = CnpjCepProbe.mapCompany(200, fixture("brasilapi-magalu.json"));
        var address = CnpjCepProbe.mapAddress(200, fixture("viacep-magalu.json"));

        assertThat(company.registrationStatus()).isEqualTo("ATIVA");
        assertThat(company.cnpj()).isEqualTo("47960950000121");
        assertThat(address.cep()).isEqualTo(company.cep());
        assertThat(cvm.findByCnpj(company.cnpj())).isEmpty();
    }

    private static ByteArrayInputStream zip(LinkedHashMap<String, String> entries) throws IOException {
        var bytes = new ByteArrayOutputStream();
        Charset charset = Charset.forName("windows-1252");
        try (var zip = new ZipOutputStream(bytes, charset)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(charset));
                zip.closeEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static String fixture(String name) throws IOException {
        try (var stream = RegistryCrossCheckProbeTests.class.getResourceAsStream("/t17/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
