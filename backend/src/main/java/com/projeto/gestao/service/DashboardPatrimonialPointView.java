package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardPatrimonialPointView(
        OffsetDateTime recordedAt,
        BigDecimal patrimonyBrl) { }
