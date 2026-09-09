package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Dados normalizados necessários para avaliar uma posição sem consultar fontes externas. */
public record PositionValuation(
        PositionBalance position,
        Currency currency,
        FinancialAmount unitMarketPrice,
        BigDecimal usdBrlRate) {
    public PositionValuation {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(unitMarketPrice, "unitMarketPrice must not be null");
        if (unitMarketPrice.value().signum() <= 0) {
            throw new IllegalArgumentException("unitMarketPrice must be greater than zero");
        }
        if (currency == Currency.USD && (usdBrlRate == null || usdBrlRate.signum() <= 0)) {
            throw new IllegalArgumentException("positive USD/BRL rate is required for USD position");
        }
        if (currency == Currency.BRL && usdBrlRate != null) {
            throw new IllegalArgumentException("USD/BRL rate must not be supplied for BRL position");
        }
    }

    public static PositionValuation brl(PositionBalance position, FinancialAmount unitMarketPrice) {
        return new PositionValuation(position, Currency.BRL, unitMarketPrice, null);
    }

    public static PositionValuation usd(PositionBalance position, FinancialAmount unitMarketPrice,
            BigDecimal usdBrlRate) {
        return new PositionValuation(position, Currency.USD, unitMarketPrice, usdBrlRate);
    }
}
