package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Estado financeiro imutável de uma posição, expresso em BRL. */
public record PositionBalance(
        PositionQuantity quantity,
        FinancialAmount totalCost,
        FinancialAmount averagePrice) {

    public PositionBalance {
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(totalCost, "totalCost must not be null");
        Objects.requireNonNull(averagePrice, "averagePrice must not be null");
        if (totalCost.value().signum() < 0 || averagePrice.value().signum() < 0) {
            throw new IllegalArgumentException("position costs must not be negative");
        }
        if (quantity.isZero() != (totalCost.value().signum() == 0 && averagePrice.value().signum() == 0)) {
            throw new IllegalArgumentException("zero position must have zero cost and average price");
        }
        if (!quantity.isZero() && (totalCost.value().signum() == 0 || averagePrice.value().signum() == 0)) {
            throw new IllegalArgumentException("open position must have positive cost and average price");
        }
    }

    public static PositionBalance empty() {
        return new PositionBalance(PositionQuantity.zero(), FinancialAmount.zero(), FinancialAmount.zero());
    }

    public boolean isOpen() {
        return !quantity.isZero();
    }

    static PositionBalance fromCost(PositionQuantity quantity, FinancialAmount totalCost) {
        if (quantity.isZero()) {
            return empty();
        }
        BigDecimal average = totalCost.value().divide(
                BigDecimal.valueOf(quantity.value()),
                FinancialAmount.SCALE,
                FinancialAmount.ROUNDING_MODE);
        return new PositionBalance(quantity, totalCost, new FinancialAmount(average));
    }
}
