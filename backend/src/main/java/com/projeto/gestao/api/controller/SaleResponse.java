package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.service.SaleResult;

public record SaleResponse(UUID assetId, UUID brokerId, String ticker, Market market,
        Currency currency, long soldQuantity, long positionQuantity,
        BigDecimal originalUnitPrice, BigDecimal unitPriceBrl, BigDecimal saleAmountBrl,
        BigDecimal realizedResultBrl, BigDecimal positionAveragePriceBrl,
        BigDecimal positionTotalCostBrl, BigDecimal remainingBalanceBrl,
        String quoteSource, Instant quoteQuotedAt, boolean quoteStale,
        BigDecimal usdBrlRate, String exchangeRateSource, Instant exchangeRateQuotedAt,
        Boolean exchangeRateStale, OffsetDateTime occurredAt) {

    static SaleResponse from(SaleResult result) {
        return new SaleResponse(result.assetId(), result.brokerAssociationId(), result.ticker(),
                result.market(), result.currency(), result.soldQuantity(),
                result.positionQuantity(), result.originalUnitPrice(), result.unitPriceBrl(),
                result.saleAmountBrl(), result.realizedResultBrl(),
                result.positionAveragePriceBrl(), result.positionTotalCostBrl(),
                result.remainingBalanceBrl(), result.quoteSource(), result.quoteQuotedAt(),
                result.quoteStale(), result.usdBrlRate(), result.exchangeRateSource(),
                result.exchangeRateQuotedAt(), result.exchangeRateStale(), result.occurredAt());
    }
}
