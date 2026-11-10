package com.projeto.gestao.domain.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminDashboardVolume(
        BigDecimal brl,
        BigDecimal usd,
        BigDecimal equivalentBrl,
        String conversionStatus,
        BigDecimal usdBrlRate,
        OffsetDateTime rateQuotedAt) { }
