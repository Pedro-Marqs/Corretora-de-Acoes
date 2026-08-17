package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor financeiro com a precisão e o arredondamento definidos pelo domínio.
 */
public record FinancialAmount(BigDecimal value) {
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public FinancialAmount {
        Objects.requireNonNull(value, "value must not be null");
        value = value.setScale(SCALE, ROUNDING_MODE);
    }

    public static FinancialAmount of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new FinancialAmount(new BigDecimal(value));
    }

    public FinancialAmount add(FinancialAmount other) {
        Objects.requireNonNull(other, "other must not be null");
        return new FinancialAmount(value.add(other.value));
    }

    public FinancialAmount multiply(long quantity) {
        return new FinancialAmount(value.multiply(BigDecimal.valueOf(quantity)));
    }

    public FinancialAmount convertUsdToBrl(BigDecimal usdBrlRate) {
        Objects.requireNonNull(usdBrlRate, "usdBrlRate must not be null");
        if (usdBrlRate.signum() <= 0) {
            throw new IllegalArgumentException("usdBrlRate must be greater than zero");
        }
        BigDecimal normalizedRate = usdBrlRate.setScale(SCALE, ROUNDING_MODE);
        return new FinancialAmount(value.multiply(normalizedRate));
    }
}
