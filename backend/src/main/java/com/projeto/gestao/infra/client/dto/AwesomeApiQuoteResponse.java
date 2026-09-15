package com.projeto.gestao.infra.client.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AwesomeApiQuoteResponse(@JsonProperty("USDBRL") Quote usdBrl) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(String code, String codein, BigDecimal bid, BigDecimal ask, Long timestamp) {
    }
}
