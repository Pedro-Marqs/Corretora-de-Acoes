package com.projeto.gestao.infra.client.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataQuoteResponse(
        String symbol,
        String name,
        String exchange,
        @JsonProperty("mic_code") String micCode,
        String currency,
        BigDecimal close,
        Long timestamp,
        Integer code,
        String message,
        String status) {
}
