package com.projeto.gestao.api.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DepositRequest(
        @NotNull(message = "Valor do aporte é obrigatório.")
        @DecimalMin(value = "10.00", message = "Valor do aporte deve ser no mínimo R$ 10,00.")
        BigDecimal amount) {
}
