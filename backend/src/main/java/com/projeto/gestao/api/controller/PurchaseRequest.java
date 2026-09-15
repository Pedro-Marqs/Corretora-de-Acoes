package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseRequest(
        @NotNull(message = "Ativo é obrigatório.") UUID assetId,
        @NotNull(message = "Corretora é obrigatória.") UUID brokerId,
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser inteira e positiva.") Long quantity,
        @JsonAlias("price") @Digits(integer = 17, fraction = 2,
                message = "Preço unitário deve ter no máximo duas casas decimais.")
        @Positive(message = "Preço unitário deve ser positivo.") BigDecimal unitPrice,
        OffsetDateTime occurredAt) { }
