package com.projeto.gestao.infra.client.probe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class CvmDatasetProbe {
    static final String DATA_ENTRY = "cad_intermed.csv";
    static final String RESPONSIBLES_ENTRY = "cad_intermed_resp.csv";
    static final String META_ENTRY = "meta_cad_intermed.txt";
    static final String CTVM_CATEGORY = "CORRETORAS";
    static final String ACTIVE_STATUS = "EM FUNCIONAMENTO NORMAL";
    private static final Charset CVM_CHARSET = Charset.forName("windows-1252");
    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "TP_PARTIC", "CNPJ", "DENOM_SOCIAL", "DENOM_COMERC", "SIT", "CD_CVM", "CEP");

    private CvmDatasetProbe() {
    }

    static Dataset inspectDataZip(InputStream input) throws IOException {
        List<String> entries = new ArrayList<>();
        List<Participant> participants = new ArrayList<>();
        try (var zip = new ZipInputStream(input, CVM_CHARSET)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.add(entry.getName());
                if (DATA_ENTRY.equals(entry.getName())) {
                    participants.addAll(readParticipants(zip));
                }
                zip.closeEntry();
            }
        }
        if (!entries.contains(DATA_ENTRY) || !entries.contains(RESPONSIBLES_ENTRY)) {
            throw new IOException("Unexpected CVM data ZIP entries: " + entries);
        }
        return new Dataset(List.copyOf(entries), List.copyOf(participants));
    }

    static String inspectMetadataZip(InputStream input) throws IOException {
        try (var zip = new ZipInputStream(input, CVM_CHARSET)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (META_ENTRY.equals(entry.getName())) {
                    String metadata = new String(zip.readAllBytes(), CVM_CHARSET);
                    for (String column : List.of("TP_PARTIC", "CNPJ", "SIT")) {
                        if (!metadata.contains("Campo: " + column)) {
                            throw new IOException("Metadata does not describe " + column);
                        }
                    }
                    return metadata;
                }
            }
        }
        throw new IOException("Missing " + META_ENTRY);
    }

    private static List<Participant> readParticipants(InputStream input) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(input, CVM_CHARSET));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IOException("Empty " + DATA_ENTRY);
        }
        List<String> header = parseCsvLine(headerLine);
        if (!header.containsAll(REQUIRED_COLUMNS)) {
            throw new IOException("Missing required CVM columns");
        }
        List<Participant> participants = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() != header.size()) {
                throw new IOException("Unexpected CVM row width");
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < header.size(); index++) {
                row.put(header.get(index), values.get(index).trim());
            }
            participants.add(new Participant(
                    row.get("TP_PARTIC"),
                    CnpjCepProbe.normalizeCnpj(row.get("CNPJ")),
                    row.get("DENOM_SOCIAL"),
                    row.get("DENOM_COMERC"),
                    row.get("SIT"),
                    row.get("CD_CVM"),
                    normalizeOptionalCep(row.get("CEP"))));
        }
        return participants;
    }

    private static String normalizeOptionalCep(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String digits = value.replaceAll("\\D", "");
        return CnpjCepProbe.normalizeCep("0".repeat(Math.max(0, 8 - digits.length())) + digits);
    }

    static List<String> parseCsvLine(String line) throws IOException {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ';' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        if (quoted) {
            throw new IOException("Unclosed quoted CVM field");
        }
        values.add(value.toString());
        return values;
    }

    record Dataset(List<String> entries, List<Participant> participants) {
        List<Participant> findByCnpj(String cnpj) {
            String normalized = CnpjCepProbe.normalizeCnpj(cnpj);
            return participants.stream().filter(item -> item.cnpj().equals(normalized)).toList();
        }
    }

    record Participant(String category, String cnpj, String legalName, String tradeName,
            String status, String cvmCode, String cep) {
        boolean isActiveCtvm() {
            return CTVM_CATEGORY.equals(category) && ACTIVE_STATUS.equals(status);
        }
    }
}
