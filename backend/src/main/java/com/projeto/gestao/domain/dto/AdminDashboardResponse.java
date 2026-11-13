package com.projeto.gestao.domain.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalAccounts,
        long activeAccounts,
        long inactiveAccounts,
        long newAccounts,
        long purchaseCount,
        long saleCount,
        long purchasedShares,
        long soldShares,
        AdminDashboardVolume purchaseVolume,
        AdminDashboardVolume saleVolume,
        List<AdminDashboardSeriesPoint> series,
        List<AdminDashboardRanking> ranking) { }
