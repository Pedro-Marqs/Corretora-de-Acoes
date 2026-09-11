package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardInvestmentPointView(
        OffsetDateTime recordedAt,
        BigDecimal positionsValueBrl) { }
