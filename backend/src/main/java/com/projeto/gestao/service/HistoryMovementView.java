package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.MovementType;

public record HistoryMovementView(
        UUID id, MovementType type, String ticker, Market market, BigDecimal quotePrice,
        BigDecimal unitPriceBrl, BigDecimal usdBrlRate, Long quantity,
        BigDecimal totalAmount, Currency currency, String brokerName,
        String originBrokerName, String destinationBrokerName, OffsetDateTime occurredAt,
        BigDecimal remainingBalance, BigDecimal realizedResult) {

    static HistoryMovementView from(Movement movement) {
        return new HistoryMovementView(movement.getId(), movement.getMovementType(),
                movement.getTicker(), movement.getMarket(), movement.getQuotePrice(),
                movement.getUnitPriceBrl(), movement.getUsdBrlRate(), movement.getQuantity(),
                movement.getTotalAmount(), movement.getCurrency(), movement.getBrokerName(),
                movement.getOriginBrokerName(), movement.getDestinationBrokerName(),
                movement.getOccurredAt(), movement.getRemainingBalance(),
                movement.getRealizedResult());
    }
}
