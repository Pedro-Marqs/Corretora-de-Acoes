package com.projeto.gestao.infra.adapter;

import com.projeto.gestao.domain.port.ExternalDataFailure;

public final class ExternalIdentifierNormalizer {

    private ExternalIdentifierNormalizer() {
    }

    public static String cnpj(String value, String source) {
        return digits(value, 14, "CNPJ", source);
    }

    public static String postalCode(String value, String source) {
        return digits(value, 8, "CEP", source);
    }

    private static String digits(String value, int length, String label, String source) {
        String normalized = value == null ? "" : value.replaceAll("\\D", "");
        if (normalized.length() != length) {
            throw new ExternalDataFailure(
                    ExternalDataFailure.Reason.INVALID_INPUT,
                    source,
                    label + " deve possuir " + length + " dígitos");
        }
        return normalized;
    }
}
