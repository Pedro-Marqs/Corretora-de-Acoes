package com.projeto.gestao.infra.client.probe;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class CnpjCepProbe {
    private static final ObjectMapper JSON = new ObjectMapper();

    private CnpjCepProbe() {
    }

    static String normalizeCnpj(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
        if (!normalized.matches("[0-9A-Z]{14}")) {
            throw new ProbeFailure(Kind.INVALID_INPUT, "CNPJ must contain 14 characters");
        }
        return normalized;
    }

    static String normalizeCep(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\D", "");
        if (!normalized.matches("\\d{8}")) {
            throw new ProbeFailure(Kind.INVALID_INPUT, "CEP must contain 8 digits");
        }
        return normalized;
    }

    static Company mapCompany(int status, String json) {
        if (status == 400) {
            throw new ProbeFailure(Kind.INVALID_INPUT, "BrasilAPI rejected the CNPJ");
        }
        if (status == 404) {
            throw new ProbeFailure(Kind.NOT_FOUND, "BrasilAPI did not find the CNPJ");
        }
        if (status < 200 || status >= 300) {
            throw new ProbeFailure(Kind.UNAVAILABLE, "BrasilAPI is unavailable");
        }
        JsonNode body = parse(json, "BrasilAPI returned invalid JSON");
        return new Company(
                normalizeCnpj(required(body, "cnpj")),
                required(body, "razao_social"),
                optional(body, "nome_fantasia"),
                required(body, "descricao_situacao_cadastral"),
                normalizeCep(required(body, "cep")));
    }

    static Address mapAddress(int status, String json) {
        if (status == 400) {
            throw new ProbeFailure(Kind.INVALID_INPUT, "ViaCEP rejected the CEP");
        }
        if (status < 200 || status >= 300) {
            throw new ProbeFailure(Kind.UNAVAILABLE, "ViaCEP is unavailable");
        }
        JsonNode body = parse(json, "ViaCEP returned invalid JSON");
        if (body.path("erro").asBoolean(false)) {
            throw new ProbeFailure(Kind.NOT_FOUND, "ViaCEP did not find the CEP");
        }
        return new Address(
                normalizeCep(required(body, "cep")),
                required(body, "logradouro"),
                optional(body, "complemento"),
                required(body, "bairro"),
                required(body, "localidade"),
                required(body, "uf"));
    }

    private static JsonNode parse(String json, String message) {
        try {
            JsonNode body = JSON.readTree(json);
            if (body == null || body.isNull()) {
                throw new ProbeFailure(Kind.INCOMPLETE_RESPONSE, message);
            }
            return body;
        } catch (IOException exception) {
            throw new ProbeFailure(Kind.INCOMPLETE_RESPONSE, message);
        }
    }

    private static String required(JsonNode body, String field) {
        String value = optional(body, field);
        if (value.isBlank()) {
            throw new ProbeFailure(Kind.INCOMPLETE_RESPONSE, "Missing field: " + field);
        }
        return value;
    }

    private static String optional(JsonNode body, String field) {
        JsonNode value = body.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText().trim();
    }

    record Company(String cnpj, String legalName, String tradeName, String registrationStatus,
            String cep) {
    }

    record Address(String cep, String street, String complement, String district, String city,
            String state) {
    }

    enum Kind {
        INVALID_INPUT, NOT_FOUND, UNAVAILABLE, INCOMPLETE_RESPONSE
    }

    static final class ProbeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final Kind kind;

        ProbeFailure(Kind kind, String message) {
            super(message);
            this.kind = kind;
        }

        Kind kind() {
            return kind;
        }
    }
}
