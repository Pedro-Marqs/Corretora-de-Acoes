package com.projeto.gestao.api.exception;

import java.util.Objects;

import com.projeto.gestao.domain.model.FinancialAmount;

public final class BusinessRuleException extends ApiException {
    private static final long serialVersionUID = 1L;

    private final FinancialAmount requested;
    private final FinancialAmount available;

    private BusinessRuleException(FinancialAmount requested, FinancialAmount available) {
        super(ApiErrorCode.BUSINESS_RULE_ERROR);
        this.requested = Objects.requireNonNull(requested, "requested must not be null");
        this.available = Objects.requireNonNull(available, "available must not be null");
    }

    public static BusinessRuleException insufficientBalance(
            FinancialAmount requested, FinancialAmount available) {
        return new BusinessRuleException(requested, available);
    }

    @Override
    public String publicMessage() {
        return "Saldo insuficiente. Valor solicitado: R$ " + requested.value().toPlainString()
                + "; saldo disponível: R$ " + available.value().toPlainString() + ".";
    }
}
