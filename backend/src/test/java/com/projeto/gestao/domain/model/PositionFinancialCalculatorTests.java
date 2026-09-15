package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class PositionFinancialCalculatorTests {
    @Test
    void firstPurchaseCreatesOpenPositionAndWeightedPurchaseUpdatesIt() {
        PositionBalance first = buy(PositionBalance.empty(), 10, "20.00");
        PositionBalance second = buy(first, 10, "30.00");

        assertPosition(first, 10, "200.00", "20.00");
        assertPosition(second, 20, "500.00", "25.00");
    }

    @Test
    void partialSalePreservesAverageAndCalculatesPositiveRealizedResult() {
        PositionBalance position = buy(buy(PositionBalance.empty(), 10, "20.00"), 10, "30.00");

        SaleResult sale = PositionFinancialCalculator.sell(position, quantity(5), money("30.00"));

        assertPosition(sale.position(), 15, "375.00", "25.00");
        assertMoney(sale.proceeds(), "150.00");
        assertMoney(sale.realizedResult(), "25.00");
    }

    @Test
    void saleBelowAverageProducesLossWithoutChangingRemainingAverage() {
        PositionBalance position = buy(PositionBalance.empty(), 10, "20.00");

        SaleResult sale = PositionFinancialCalculator.sell(position, quantity(4), money("17.50"));

        assertPosition(sale.position(), 6, "120.00", "20.00");
        assertMoney(sale.realizedResult(), "-10.00");
    }

    @Test
    void totalSaleZerosEveryOpenPositionValueAndRepurchaseStartsANewCostBasis() {
        PositionBalance original = buy(PositionBalance.empty(), 3, "19.99");
        SaleResult sale = PositionFinancialCalculator.sell(original, quantity(3), money("25.00"));
        PositionBalance repurchase = buy(sale.position(), 2, "40.00");

        assertThat(sale.position()).isEqualTo(PositionBalance.empty());
        assertThat(sale.position().isOpen()).isFalse();
        assertMoney(sale.realizedResult(), "15.03");
        assertPosition(repurchase, 2, "80.00", "40.00");
        assertPosition(original, 3, "59.97", "19.99");
    }

    @Test
    void rejectsInvalidPositionStatesAndOperationsWithoutMutatingInputs() {
        PositionBalance position = buy(PositionBalance.empty(), 5, "10.00");

        assertThatIllegalArgumentException().isThrownBy(() -> quantity(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PositionQuantity(-1));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new PositionBalance(PositionQuantity.zero(), money("1.00"), money("1.00")));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new PositionBalance(new PositionQuantity(1), FinancialAmount.zero(), FinancialAmount.zero()));
        assertThatIllegalArgumentException().isThrownBy(() ->
                PositionFinancialCalculator.sell(position, quantity(6), money("10.00")));
        assertThatIllegalArgumentException().isThrownBy(() ->
                PositionFinancialCalculator.buy(position, new PositionQuantity(0), money("10.00")));
        assertPosition(position, 5, "50.00", "10.00");
    }

    @Test
    void transfersPartOfPositionToEmptyDestinationAndConservesCost() {
        BrokerPosition origin = broker("origin", buy(PositionBalance.empty(), 10, "20.00"));
        BrokerPosition destination = broker("destination", PositionBalance.empty());

        PositionTransferResult result = PositionFinancialCalculator.transfer(origin, destination, quantity(4));

        assertPosition(result.origin().balance(), 6, "120.00", "20.00");
        assertPosition(result.destination().balance(), 4, "80.00", "20.00");
        assertMoney(result.transferredCost(), "80.00");
        assertMoney(result.realizedResult(), "0.00");
        assertMoney(result.origin().balance().totalCost().add(result.destination().balance().totalCost()), "200.00");
        assertPosition(origin.balance(), 10, "200.00", "20.00");
    }

    @Test
    void transfersIntoExistingPositionUsingWeightedAverage() {
        BrokerPosition origin = broker("origin", buy(PositionBalance.empty(), 5, "20.00"));
        BrokerPosition destination = broker("destination", buy(PositionBalance.empty(), 5, "30.00"));

        PositionTransferResult result = PositionFinancialCalculator.transfer(origin, destination, quantity(5));

        assertThat(result.origin().balance()).isEqualTo(PositionBalance.empty());
        assertPosition(result.destination().balance(), 10, "250.00", "25.00");
        assertMoney(result.origin().balance().totalCost().add(result.destination().balance().totalCost()), "250.00");
    }

    @Test
    void partialAndTotalTransfersConserveRoundingRemainder() {
        BrokerPosition origin = broker("a", buy(PositionBalance.empty(), 3, "10.00"));
        PositionTransferResult partial = PositionFinancialCalculator.transfer(
                origin, broker("b", PositionBalance.empty()), quantity(1));
        PositionTransferResult totalRemainder = PositionFinancialCalculator.transfer(
                partial.origin(), broker("c", PositionBalance.empty()), quantity(2));

        assertMoney(partial.transferredCost(), "10.00");
        assertMoney(totalRemainder.transferredCost(), "20.00");
        assertThat(totalRemainder.origin().balance()).isEqualTo(PositionBalance.empty());
    }

    @Test
    void invalidTransferRejectsExcessAndSameBrokerWithoutChangingValues() {
        BrokerPosition origin = broker("same", buy(PositionBalance.empty(), 5, "20.00"));
        BrokerPosition other = broker("other", PositionBalance.empty());

        assertThatIllegalArgumentException().isThrownBy(() ->
                PositionFinancialCalculator.transfer(origin, other, quantity(6)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                PositionFinancialCalculator.transfer(origin, broker("same", PositionBalance.empty()), quantity(1)));
        assertPosition(origin.balance(), 5, "100.00", "20.00");
        assertThat(other.balance()).isEqualTo(PositionBalance.empty());
    }

    @Test
    void convertsInternationalOperationAndMarketValueWithoutFees() {
        FinancialAmount operation = money("10.00").multiply(2).convertUsdToBrl(new BigDecimal("5.00"));
        PositionValuation valuation = PositionValuation.usd(
                buy(PositionBalance.empty(), 2, "50.00"), money("20.00"), new BigDecimal("5.00"));

        assertMoney(operation, "100.00");
        assertMoney(PositionFinancialCalculator.marketValue(valuation), "200.00");
    }

    @Test
    void roundsOperationAndValuationAtThirdDecimalUsingHalfUp() {
        assertMoney(money("10.124"), "10.12");
        assertMoney(money("10.125"), "10.13");
        PositionBalance position = buy(PositionBalance.empty(), 1, "1.00");
        assertMoney(PositionFinancialCalculator.marketValue(
                PositionValuation.usd(position, money("2.01"), new BigDecimal("2.345"))), "4.72");
    }

    @Test
    void consolidatesBrazilianAndUsdPositionsAcrossBrokersAndAccumulatedResults() {
        PositionBalance brazil = buy(PositionBalance.empty(), 10, "20.00");
        PositionBalance usa = buy(PositionBalance.empty(), 2, "50.00");
        List<PositionValuation> positions = List.of(
                PositionValuation.brl(brazil, money("25.00")),
                PositionValuation.usd(usa, money("12.00"), new BigDecimal("5.00")));

        InvestmentResults result = PositionFinancialCalculator.consolidate(
                money("1000.00"), money("25.00").add(money("-10.00")), positions);

        assertMoney(result.marketValue(), "370.00");
        assertMoney(result.patrimony(), "1370.00");
        assertMoney(result.realizedResult(), "15.00");
        assertMoney(result.unrealizedResult(), "70.00");
        assertMoney(result.totalResult(), "85.00");
    }

    @Test
    void initialBalanceAndDepositsChangePatrimonyButNeverInvestmentResults() {
        InvestmentResults initial = PositionFinancialCalculator.consolidate(
                money("10000.00"), FinancialAmount.zero(), List.of());
        InvestmentResults afterDeposit = PositionFinancialCalculator.consolidate(
                money("10500.00"), initial.realizedResult(), List.of());
        PositionValuation unchangedPosition = PositionValuation.brl(
                buy(PositionBalance.empty(), 10, "20.00"), money("20.00"));
        InvestmentResults invested = PositionFinancialCalculator.consolidate(
                money("9800.00"), FinancialAmount.zero(), List.of(unchangedPosition));
        InvestmentResults investedAfterDeposit = PositionFinancialCalculator.consolidate(
                money("10300.00"), invested.realizedResult(), List.of(unchangedPosition));

        assertMoney(initial.patrimony(), "10000.00");
        assertMoney(afterDeposit.patrimony(), "10500.00");
        assertInvestmentResultsZero(initial);
        assertInvestmentResultsZero(afterDeposit);
        assertInvestmentResultsZero(invested);
        assertInvestmentResultsZero(investedAfterDeposit);
        assertMoney(investedAfterDeposit.patrimony().subtract(invested.patrimony()), "500.00");
    }

    @Test
    void patrimonyIsBalancePlusMarketValue() {
        PositionBalance position = buy(PositionBalance.empty(), 10, "200.00");
        InvestmentResults result = PositionFinancialCalculator.consolidate(
                money("1000.00"), FinancialAmount.zero(),
                List.of(PositionValuation.brl(position, money("250.00"))));

        assertMoney(result.marketValue(), "2500.00");
        assertMoney(result.patrimony(), "3500.00");
    }

    private static PositionBalance buy(PositionBalance current, long quantity, String price) {
        return PositionFinancialCalculator.buy(current, quantity(quantity), money(price));
    }

    private static BrokerPosition broker(String id, PositionBalance position) {
        return new BrokerPosition(id, position);
    }

    private static PositionQuantity quantity(long value) {
        return PositionQuantity.positive(value);
    }

    private static FinancialAmount money(String value) {
        return FinancialAmount.of(value);
    }

    private static void assertPosition(PositionBalance position, long quantity, String totalCost,
            String averagePrice) {
        assertThat(position.quantity().value()).isEqualTo(quantity);
        assertMoney(position.totalCost(), totalCost);
        assertMoney(position.averagePrice(), averagePrice);
    }

    private static void assertMoney(FinancialAmount actual, String expected) {
        assertThat(actual.value()).isEqualByComparingTo(expected);
    }

    private static void assertInvestmentResultsZero(InvestmentResults results) {
        assertMoney(results.realizedResult(), "0.00");
        assertMoney(results.unrealizedResult(), "0.00");
        assertMoney(results.totalResult(), "0.00");
    }
}
