package com.projeto.gestao.api.controller;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WithdrawalRequest(
        @NotNull(message = "Valor da retirada é obrigatório.")
        @Positive(message = "Valor da retirada deve ser positivo.") BigDecimal amount) { }
