package com.projeto.gestao.api.exception;

import java.util.Objects;

import com.projeto.gestao.domain.model.FinancialAmount;

public final class BusinessRuleException extends ApiException {
    private static final long serialVersionUID = 1L;

    private final FinancialAmount requested;
    private final FinancialAmount available;
    private final Long requestedQuantity;
    private final Long availableQuantity;

    private BusinessRuleException(FinancialAmount requested, FinancialAmount available) {
        super(ApiErrorCode.BUSINESS_RULE_ERROR);
        this.requested = Objects.requireNonNull(requested, "requested must not be null");
        this.available = Objects.requireNonNull(available, "available must not be null");
        this.requestedQuantity = null;
        this.availableQuantity = null;
    }

    private BusinessRuleException(long requestedQuantity, long availableQuantity) {
        super(ApiErrorCode.BUSINESS_RULE_ERROR);
        this.requested = null;
        this.available = null;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public static BusinessRuleException insufficientBalance(
            FinancialAmount requested, FinancialAmount available) {
        return new BusinessRuleException(requested, available);
    }

    public static BusinessRuleException insufficientPosition(
            long requestedQuantity, long availableQuantity) {
        return new BusinessRuleException(requestedQuantity, availableQuantity);
    }

    @Override
    public String publicMessage() {
        if (requestedQuantity != null) {
            return "Posição insuficiente. Quantidade solicitada: " + requestedQuantity
                    + "; quantidade disponível: " + availableQuantity + ".";
        }
        return "Saldo insuficiente. Valor solicitado: R$ " + requested.value().toPlainString()
                + "; saldo disponível: R$ " + available.value().toPlainString() + ".";
    }
}
