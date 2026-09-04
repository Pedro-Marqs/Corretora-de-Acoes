package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.service.TransferResult;

public record TransferResponse(UUID assetId, UUID originBrokerId, UUID destinationBrokerId,
        String ticker, Market market, Currency currency, long transferredQuantity,
        long originQuantity, BigDecimal originAveragePriceBrl, BigDecimal originTotalCostBrl,
        long destinationQuantity, BigDecimal destinationAveragePriceBrl,
        BigDecimal destinationTotalCostBrl, BigDecimal transferredCostBrl,
        BigDecimal remainingBalanceBrl, OffsetDateTime occurredAt) {
    static TransferResponse from(TransferResult result) {
        return new TransferResponse(result.assetId(), result.originBrokerId(),
                result.destinationBrokerId(), result.ticker(), result.market(), result.currency(),
                result.transferredQuantity(), result.originQuantity(),
                result.originAveragePriceBrl(), result.originTotalCostBrl(),
                result.destinationQuantity(), result.destinationAveragePriceBrl(),
                result.destinationTotalCostBrl(), result.transferredCostBrl(),
                result.remainingBalanceBrl(), result.occurredAt());
    }
}
