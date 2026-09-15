package com.projeto.gestao.domain.model;

import java.util.Objects;

public record InvestmentResults(
        FinancialAmount balance,
        FinancialAmount marketValue,
        FinancialAmount patrimony,
        FinancialAmount realizedResult,
        FinancialAmount unrealizedResult,
        FinancialAmount totalResult) {
    public InvestmentResults {
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(marketValue, "marketValue must not be null");
        Objects.requireNonNull(patrimony, "patrimony must not be null");
        Objects.requireNonNull(realizedResult, "realizedResult must not be null");
        Objects.requireNonNull(unrealizedResult, "unrealizedResult must not be null");
        Objects.requireNonNull(totalResult, "totalResult must not be null");
    }
}
