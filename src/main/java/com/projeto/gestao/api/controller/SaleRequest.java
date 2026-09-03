package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaleRequest(
        @NotNull(message = "Ativo é obrigatório.") UUID assetId,
        @NotNull(message = "Corretora é obrigatória.") UUID brokerId,
        @NotNull(message = "Quantidade é obrigatória.")
        @Positive(message = "Quantidade deve ser inteira e positiva.")
        @Digits(integer = 18, fraction = 0, message = "Quantidade deve ser inteira e positiva.")
        BigDecimal quantity) {
    public long quantityAsLong() {
        return quantity.longValueExact();
    }
}
