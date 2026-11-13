package com.projeto.gestao.domain.dto;

import java.time.OffsetDateTime;

import com.projeto.gestao.domain.model.MovementType;

public interface AdminDashboardMovementDayProjection {
    OffsetDateTime getOccurredAt();
    MovementType getMovementType();
}
