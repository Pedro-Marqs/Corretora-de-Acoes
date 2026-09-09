package com.projeto.gestao.infra.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;

import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.adapter.ExternalIdentifierNormalizer;
import com.projeto.gestao.infra.client.dto.CvmParticipantRow;

@Component
public class CvmDatasetParser {

    static final String DATA_ENTRY = "cad_intermed.csv";
    private static final Charset CHARSET = Charset.forName("windows-1252");
    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "TP_PARTIC", "CNPJ", "DENOM_SOCIAL", "DENOM_COMERC", "SIT", "CD_CVM", "CEP");

    public Map<String, List<CvmParticipantRow>> parse(byte[] content) {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(content), CHARSET)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (DATA_ENTRY.equals(entry.getName())) {
                    return index(readRows(zip));
                }
            }
            throw invalid("Arquivo " + DATA_ENTRY + " ausente no ZIP", null);
        } catch (IOException exception) {
            throw invalid("Não foi possível interpretar o dataset da CVM", exception);
        }
    }

    private static List<CvmParticipantRow> readRows(ZipInputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, CHARSET));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IOException("CSV vazio");
        }
        List<String> header = parseCsvLine(headerLine);
        if (!header.containsAll(REQUIRED_COLUMNS)) {
            throw new IOException("Colunas obrigatórias ausentes");
        }

        List<CvmParticipantRow> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() != header.size()) {
                throw new IOException("Quantidade de colunas inválida");
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < header.size(); index++) {
                row.put(header.get(index), values.get(index).trim());
            }
            String cnpj;
            try {
                cnpj = ExternalIdentifierNormalizer.cnpj(row.get("CNPJ"), CvmDatasetClient.SOURCE);
            } catch (ExternalDataFailure exception) {
                throw new IOException("CNPJ inválido no dataset", exception);
            }
            require(row, "TP_PARTIC");
            require(row, "DENOM_SOCIAL");
            require(row, "SIT");
            rows.add(new CvmParticipantRow(row.get("TP_PARTIC"), cnpj, row.get("DENOM_SOCIAL"),
                    row.get("DENOM_COMERC"), row.get("SIT"), row.get("CD_CVM")));
        }
        if (rows.isEmpty()) {
            throw new IOException("Dataset sem participantes");
        }
        return rows;
    }

    private static void require(Map<String, String> row, String field) throws IOException {
        if (row.get(field) == null || row.get(field).isBlank()) {
            throw new IOException("Campo obrigatório vazio: " + field);
        }
    }

    private static Map<String, List<CvmParticipantRow>> index(List<CvmParticipantRow> rows) {
        Map<String, List<CvmParticipantRow>> mutable = new LinkedHashMap<>();
        for (CvmParticipantRow row : rows) {
            mutable.computeIfAbsent(row.cnpj(), ignored -> new ArrayList<>()).add(row);
        }
        Map<String, List<CvmParticipantRow>> immutable = new LinkedHashMap<>();
        mutable.forEach((cnpj, participants) -> immutable.put(cnpj, List.copyOf(participants)));
        return Map.copyOf(immutable);
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
            throw new IOException("Campo CSV sem fechamento");
        }
        values.add(value.toString());
        return values;
    }

    private static ExternalDataFailure invalid(String message, Throwable cause) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_RESPONSE,
                CvmDatasetClient.SOURCE, message, cause);
    }
}
