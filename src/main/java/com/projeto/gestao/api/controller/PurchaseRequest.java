package com.projeto.gestao.api.controller;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
        @NotNull(message = "Ativo é obrigatório.") UUID assetId,
        @NotNull(message = "Corretora é obrigatória.") UUID brokerId,
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser inteira e positiva.") Long quantity) { }
