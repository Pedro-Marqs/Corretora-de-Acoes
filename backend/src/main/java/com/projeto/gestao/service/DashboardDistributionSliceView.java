package com.projeto.gestao.service;

import java.math.BigDecimal;

public record DashboardDistributionSliceView(
        String identifier,
        String label,
        BigDecimal valueBrl) { }
