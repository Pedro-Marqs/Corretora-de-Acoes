package com.projeto.gestao.service;

import java.util.List;

public record DashboardDistributionsView(
        List<DashboardDistributionSliceView> byAsset,
        List<DashboardDistributionSliceView> byBroker,
        List<DashboardDistributionSliceView> byMarket) { }
