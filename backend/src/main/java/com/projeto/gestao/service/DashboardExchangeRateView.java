package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardExchangeRateView(
        String currencyPair,
        BigDecimal rate,
        OffsetDateTime quotedAt,
        boolean stale) { }
