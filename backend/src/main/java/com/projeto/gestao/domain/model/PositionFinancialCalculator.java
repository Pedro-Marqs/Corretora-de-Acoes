package com.projeto.gestao.domain.model;

import java.util.Collection;
import java.util.Objects;

/** Regras financeiras puras para posições e consolidação de carteira. */
public final class PositionFinancialCalculator {
    private PositionFinancialCalculator() {
    }

    public static PositionBalance buy(PositionBalance current, PositionQuantity quantity,
            FinancialAmount unitPriceBrl) {
        requirePositiveOperation(current, quantity, unitPriceBrl);
        FinancialAmount purchaseCost = unitPriceBrl.multiply(quantity.value());
        PositionQuantity resultingQuantity = current.quantity().add(quantity);
        return PositionBalance.fromCost(resultingQuantity, current.totalCost().add(purchaseCost));
    }

    public static SaleResult sell(PositionBalance current, PositionQuantity quantity,
            FinancialAmount unitSalePriceBrl) {
        requirePositiveOperation(current, quantity, unitSalePriceBrl);
        if (!current.isOpen()) {
            throw new IllegalArgumentException("cannot sell an empty position");
        }
        PositionQuantity remainingQuantity = current.quantity().subtract(quantity);
        FinancialAmount removedCost = remainingQuantity.isZero()
                ? current.totalCost()
                : current.averagePrice().multiply(quantity.value());
        PositionBalance remaining = remainingQuantity.isZero()
                ? PositionBalance.empty()
                : new PositionBalance(remainingQuantity,
                        current.totalCost().subtract(removedCost), current.averagePrice());
        FinancialAmount proceeds = unitSalePriceBrl.multiply(quantity.value());
        return new SaleResult(remaining, proceeds, proceeds.subtract(removedCost));
    }

    public static PositionTransferResult transfer(BrokerPosition origin, BrokerPosition destination,
            PositionQuantity quantity) {
        Objects.requireNonNull(origin, "origin must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        if (origin.brokerId().equals(destination.brokerId())) {
            throw new IllegalArgumentException("origin and destination brokers must differ");
        }
        if (quantity.isZero() || !origin.balance().isOpen()) {
            throw new IllegalArgumentException("transfer quantity and origin position must be positive");
        }
        PositionQuantity originRemainingQuantity = origin.balance().quantity().subtract(quantity);
        FinancialAmount transferredCost = originRemainingQuantity.isZero()
                ? origin.balance().totalCost()
                : origin.balance().averagePrice().multiply(quantity.value());
        PositionBalance originRemaining = originRemainingQuantity.isZero()
                ? PositionBalance.empty()
                : new PositionBalance(originRemainingQuantity,
                        origin.balance().totalCost().subtract(transferredCost), origin.balance().averagePrice());
        PositionBalance destinationResult = PositionBalance.fromCost(
                destination.balance().quantity().add(quantity),
                destination.balance().totalCost().add(transferredCost));
        return new PositionTransferResult(
                new BrokerPosition(origin.brokerId(), originRemaining),
                new BrokerPosition(destination.brokerId(), destinationResult),
                transferredCost,
                FinancialAmount.zero());
    }

    public static FinancialAmount marketValue(PositionValuation valuation) {
        Objects.requireNonNull(valuation, "valuation must not be null");
        FinancialAmount nativeValue = valuation.unitMarketPrice().multiply(valuation.position().quantity().value());
        return valuation.currency() == Currency.USD
                ? nativeValue.convertUsdToBrl(valuation.usdBrlRate())
                : nativeValue;
    }

    public static InvestmentResults consolidate(FinancialAmount balance, FinancialAmount realizedResult,
            Collection<PositionValuation> positions) {
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(realizedResult, "realizedResult must not be null");
        Objects.requireNonNull(positions, "positions must not be null");
        FinancialAmount marketValue = FinancialAmount.zero();
        FinancialAmount totalCost = FinancialAmount.zero();
        for (PositionValuation valuation : positions) {
            Objects.requireNonNull(valuation, "positions must not contain null");
            marketValue = marketValue.add(marketValue(valuation));
            totalCost = totalCost.add(valuation.position().totalCost());
        }
        FinancialAmount unrealized = marketValue.subtract(totalCost);
        return new InvestmentResults(balance, marketValue, balance.add(marketValue), realizedResult,
                unrealized, realizedResult.add(unrealized));
    }

    private static void requirePositiveOperation(PositionBalance current, PositionQuantity quantity,
            FinancialAmount unitPrice) {
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity.isZero() || unitPrice.value().signum() <= 0) {
            throw new IllegalArgumentException("quantity and unit price must be greater than zero");
        }
    }
}
