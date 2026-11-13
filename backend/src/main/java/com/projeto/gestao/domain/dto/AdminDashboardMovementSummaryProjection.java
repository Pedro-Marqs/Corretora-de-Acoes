package com.projeto.gestao.domain.dto;

import java.math.BigDecimal;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.MovementType;

public interface AdminDashboardMovementSummaryProjection {
    MovementType getMovementType();
    Currency getCurrency();
    Long getOperationCount();
    Long getShares();
    BigDecimal getNativeVolume();
}
