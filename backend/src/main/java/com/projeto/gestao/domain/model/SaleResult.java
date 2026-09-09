package com.projeto.gestao.domain.model;

import java.util.Objects;

public record SaleResult(
        PositionBalance position,
        FinancialAmount proceeds,
        FinancialAmount realizedResult) {
    public SaleResult {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(proceeds, "proceeds must not be null");
        Objects.requireNonNull(realizedResult, "realizedResult must not be null");
    }
}
