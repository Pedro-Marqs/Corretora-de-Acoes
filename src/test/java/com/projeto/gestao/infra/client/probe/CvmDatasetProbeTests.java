package com.projeto.gestao.infra.client.probe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class CvmDatasetProbeTests {
    private static final Charset CVM_CHARSET = Charset.forName("windows-1252");

    @Test
    void inspectsOfficialZipShapeAndFindsCtvmByNormalizedCnpj() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(CvmDatasetProbe.DATA_ENTRY, fixture("cvm-participants.csv"));
        entries.put(CvmDatasetProbe.RESPONSIBLES_ENTRY, "TP_PARTIC;CNPJ;DENOM_SOCIAL\n");

        var dataset = CvmDatasetProbe.inspectDataZip(zip(entries));
        var xp = dataset.findByCnpj("02.332.886/0001-04");

        assertThat(dataset.entries()).containsExactly("cad_intermed.csv", "cad_intermed_resp.csv");
        assertThat(xp).hasSize(3);
        assertThat(xp).filteredOn(CvmDatasetProbe.Participant::isActiveCtvm)
                .singleElement()
                .satisfies(participant -> {
                    assertThat(participant.category()).isEqualTo("CORRETORAS");
                    assertThat(participant.status()).isEqualTo("EM FUNCIONAMENTO NORMAL");
                    assertThat(participant.cvmCode()).isEqualTo("3247");
                });
    }

    @Test
    void verifiesDictionaryDocumentsRegulatoryColumns() throws IOException {
        var metadata = CvmDatasetProbe.inspectMetadataZip(zip(Map.of(
                CvmDatasetProbe.META_ENTRY, fixture("meta-cad-intermed.txt"))));

        assertThat(metadata).contains("Campo: TP_PARTIC", "Tipo de participante",
                "Campo: CNPJ", "Campo: SIT");
    }

    @Test
    void supportsQuotedSemicolonAndRejectsUnexpectedContracts() throws IOException {
        assertThat(CvmDatasetProbe.parseCsvLine("CORRETORAS;\"EMPRESA; S.A.\";ATIVA"))
                .containsExactly("CORRETORAS", "EMPRESA; S.A.", "ATIVA");
        assertThatThrownBy(() -> CvmDatasetProbe.inspectDataZip(zip(Map.of(
                CvmDatasetProbe.DATA_ENTRY, "CNPJ;SIT\n"))))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> CvmDatasetProbe.inspectMetadataZip(zip(Map.of(
                CvmDatasetProbe.META_ENTRY, "Campo: CNPJ"))))
                .isInstanceOf(IOException.class);
    }

    private static ByteArrayInputStream zip(Map<String, String> entries) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes, CVM_CHARSET)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(CVM_CHARSET));
                zip.closeEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static String fixture(String name) throws IOException {
        try (var stream = CvmDatasetProbeTests.class.getResourceAsStream("/t17/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return new String(stream.readAllBytes(), CVM_CHARSET);
        }
    }
}
