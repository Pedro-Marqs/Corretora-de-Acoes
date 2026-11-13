package com.projeto.gestao.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.AccountStatus;

public interface AdminInvestorProjection {
    UUID getId();
    String getName();
    String getEmail();
    String getCpf();
    AccountStatus getStatus();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getInactivatedAt();
}
