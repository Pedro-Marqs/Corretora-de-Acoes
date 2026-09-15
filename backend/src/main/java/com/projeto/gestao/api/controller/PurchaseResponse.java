package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.service.PurchaseResult;

public record PurchaseResponse(UUID assetId, UUID brokerId, String ticker, Market market,
        Currency currency, long purchasedQuantity, long positionQuantity,
        BigDecimal originalUnitPrice, BigDecimal unitPriceBrl, BigDecimal purchaseAmountBrl,
        BigDecimal positionAveragePriceBrl, BigDecimal positionTotalCostBrl,
        BigDecimal remainingBalanceBrl, String quoteSource, Instant quoteQuotedAt,
        boolean quoteStale, BigDecimal usdBrlRate, String exchangeRateSource,
        Instant exchangeRateQuotedAt, Boolean exchangeRateStale, OffsetDateTime occurredAt) {

    static PurchaseResponse from(PurchaseResult result) {
        return new PurchaseResponse(result.assetId(), result.brokerAssociationId(), result.ticker(),
                result.market(), result.currency(), result.purchasedQuantity(),
                result.positionQuantity(), result.originalUnitPrice(), result.unitPriceBrl(),
                result.purchaseAmountBrl(), result.positionAveragePriceBrl(),
                result.positionTotalCostBrl(), result.remainingBalanceBrl(), result.quoteSource(),
                result.quoteQuotedAt(), result.quoteStale(), result.usdBrlRate(),
                result.exchangeRateSource(), result.exchangeRateQuotedAt(),
                result.exchangeRateStale(), result.occurredAt());
    }
}
