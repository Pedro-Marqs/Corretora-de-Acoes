package com.projeto.gestao.domain.model;

import java.util.Objects;

public record PositionTransferResult(
        BrokerPosition origin,
        BrokerPosition destination,
        FinancialAmount transferredCost,
        FinancialAmount realizedResult) {
    public PositionTransferResult {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(transferredCost, "transferredCost must not be null");
        Objects.requireNonNull(realizedResult, "realizedResult must not be null");
    }
}
