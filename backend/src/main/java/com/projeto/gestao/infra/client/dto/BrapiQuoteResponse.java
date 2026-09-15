package com.projeto.gestao.infra.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiQuoteResponse(List<Result> results) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String requestedSymbol, String symbol, Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String shortName, String longName, String currency,
            BigDecimal regularMarketPrice, Instant regularMarketTime) {
    }
}
