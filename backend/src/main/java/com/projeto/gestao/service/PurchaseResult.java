package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;

public record PurchaseResult(UUID assetId, UUID brokerAssociationId, String ticker,
        Market market, Currency currency, long purchasedQuantity, long positionQuantity,
        BigDecimal originalUnitPrice, BigDecimal unitPriceBrl, BigDecimal purchaseAmountBrl,
        BigDecimal positionAveragePriceBrl, BigDecimal positionTotalCostBrl,
        BigDecimal remainingBalanceBrl, String quoteSource, Instant quoteQuotedAt,
        boolean quoteStale, BigDecimal usdBrlRate, String exchangeRateSource,
        Instant exchangeRateQuotedAt, Boolean exchangeRateStale, OffsetDateTime occurredAt) { }
