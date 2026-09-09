package com.projeto.gestao.service;

import java.math.BigDecimal;

public record PatrimonyCalculation(
        BigDecimal balanceBrl,
        BigDecimal positionsValueBrl,
        BigDecimal usdBrlRate,
        BigDecimal patrimonyBrl) {
}
