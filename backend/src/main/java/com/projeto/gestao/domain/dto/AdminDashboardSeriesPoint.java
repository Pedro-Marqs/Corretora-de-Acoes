package com.projeto.gestao.domain.dto;

import java.time.LocalDate;

public record AdminDashboardSeriesPoint(
        LocalDate date,
        long newAccounts,
        long purchaseCount,
        long saleCount) { }
