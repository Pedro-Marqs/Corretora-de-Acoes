package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BrokerAssociationRequest(
        @NotBlank(message = "CNPJ é obrigatório.")
        @Pattern(regexp = "(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})",
                message = "CNPJ deve possuir 14 dígitos.")
        String cnpj) {
}
