package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Instant;

public record CachedExchangeRate(BigDecimal rate, String source, Instant quotedAt,
        Instant collectedAt, boolean stale) { }
