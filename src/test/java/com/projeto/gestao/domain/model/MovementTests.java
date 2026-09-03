package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MovementTests {
    private final Account account = Account.create(UUID.randomUUID(), "Test", "52998224725",
            "test@example.com", "hash", new BigDecimal("10000.00"), OffsetDateTime.now());
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-26T10:00:00-03:00");

    @Test
    void createsEachMovementTypeWithOnlyApplicableFields() {
        Movement initial = Movement.initialBalance(UUID.randomUUID(), account,
                new BigDecimal("10000"), now);
        Movement deposit = Movement.deposit(UUID.randomUUID(), account,
                new BigDecimal("10"), new BigDecimal("10010"), now);
        Movement purchase = Movement.purchase(UUID.randomUUID(), account, "PETR4", Market.BR,
                new BigDecimal("20"), new BigDecimal("20"), null, 2,
                new BigDecimal("40"), Currency.BRL,
                "Broker", new BigDecimal("9970"), now);
        Movement sale = Movement.sale(UUID.randomUUID(), account, "AAPL", Market.US,
                new BigDecimal("10"), 2, new BigDecimal("20"), Currency.USD,
                "Broker", new BigDecimal("10070"), new BigDecimal("-5"), now);
        Movement usPurchase = Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                new BigDecimal("10.005"), new BigDecimal("50.15"),
                new BigDecimal("5.005"), 2, new BigDecimal("100.30"), Currency.USD,
                "Broker", new BigDecimal("9869.70"), now);
        Movement transfer = Movement.transfer(UUID.randomUUID(), account, "PETR4", Market.BR,
                2, new BigDecimal("40"), Currency.BRL, "Origin", "Destination",
                new BigDecimal("10070"), now);

        assertThat(initial.getMovementType()).isEqualTo(MovementType.INITIAL_BALANCE);
        assertThat(deposit.getMovementType()).isEqualTo(MovementType.DEPOSIT);
        assertThat(purchase.getBrokerName()).isEqualTo("Broker");
        assertThat(purchase.getUnitPriceBrl()).isEqualByComparingTo("20.00");
        assertThat(purchase.getUsdBrlRate()).isNull();
        assertThat(usPurchase.getUnitPriceBrl()).isEqualByComparingTo("50.15");
        assertThat(usPurchase.getUsdBrlRate()).isEqualByComparingTo("5.01");
        assertThat(purchase.getOriginBrokerName()).isNull();
        assertThat(sale.getRealizedResult()).isEqualByComparingTo("-5.00");
        assertThat(transfer.getQuotePrice()).isNull();
        assertThat(transfer.getBrokerName()).isNull();
        assertThat(transfer.getOriginBrokerName()).isEqualTo("Origin");
    }

    @Test
    void rejectsMissingInvalidOrInconsistentFields() {
        assertThatThrownBy(() -> Movement.deposit(UUID.randomUUID(), account, BigDecimal.ZERO,
                new BigDecimal("10000"), now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.purchase(UUID.randomUUID(), account, " ", Market.BR,
                BigDecimal.TEN, BigDecimal.TEN, null, 1, BigDecimal.TEN, Currency.BRL,
                "Broker", BigDecimal.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.initialBalance(UUID.randomUUID(), account,
                new BigDecimal("0.004"), now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 0, BigDecimal.TEN,
                Currency.USD, "Broker", BigDecimal.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 1, BigDecimal.TEN,
                Currency.BRL, "Broker", BigDecimal.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                BigDecimal.TEN, BigDecimal.TEN, null, 1, BigDecimal.TEN,
                Currency.USD, "Broker", BigDecimal.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.purchase(UUID.randomUUID(), account, "AAPL", Market.US,
                BigDecimal.TEN, new BigDecimal("49.99"), new BigDecimal("5.00"), 1,
                new BigDecimal("49.99"), Currency.USD, "Broker", BigDecimal.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Movement.transfer(UUID.randomUUID(), account, "PETR4", Market.BR,
                1, BigDecimal.TEN, Currency.BRL, "Same", "Same", BigDecimal.ZERO, now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsConsistentImmutablePatrimonialPoint() {
        Movement movement = Movement.initialBalance(UUID.randomUUID(), account,
                new BigDecimal("10000"), now);
        PatrimonialPoint point = PatrimonialPoint.create(UUID.randomUUID(), account, movement,
                new BigDecimal("9000"), new BigDecimal("1000"), new BigDecimal("5"),
                new BigDecimal("10000"), now);

        assertThat(point.getBalanceBrl()).isEqualByComparingTo("9000.00");
        assertThat(point.getPositionsValueBrl()).isEqualByComparingTo("1000.00");
        assertThat(point.getUsdBrlRate()).isEqualByComparingTo("5.00");
        assertThatThrownBy(() -> PatrimonialPoint.create(UUID.randomUUID(), account, movement,
                BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
