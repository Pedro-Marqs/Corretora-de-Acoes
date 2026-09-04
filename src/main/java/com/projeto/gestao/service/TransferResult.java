package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record TransferResult(UUID assetId, UUID originBrokerId, UUID destinationBrokerId,
        String ticker, Market market, Currency currency, long transferredQuantity,
        long originQuantity, BigDecimal originAveragePriceBrl, BigDecimal originTotalCostBrl,
        long destinationQuantity, BigDecimal destinationAveragePriceBrl,
        BigDecimal destinationTotalCostBrl, BigDecimal transferredCostBrl,
        BigDecimal remainingBalanceBrl, OffsetDateTime occurredAt) { }
