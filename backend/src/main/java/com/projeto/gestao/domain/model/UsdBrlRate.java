package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record UsdBrlRate(BigDecimal rate, Instant quotedAt, Instant collectedAt, String source) {
}
