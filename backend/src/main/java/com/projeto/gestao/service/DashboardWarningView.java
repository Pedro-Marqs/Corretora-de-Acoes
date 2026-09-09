package com.projeto.gestao.service;

import java.time.OffsetDateTime;

public record DashboardWarningView(
        DashboardWarningType type,
        String ticker,
        OffsetDateTime observedAt) { }
