package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PositionTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T09:00:00-03:00");
    private final Account account = Account.create(UUID.randomUUID(), "Investor", "12345678901",
            "position@example.com", "hash", new BigDecimal("1000.00"), NOW);
    private final Asset asset = new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL);
    private final AccountBroker association = AccountBroker.create(UUID.randomUUID(), account,
            Broker.create(UUID.randomUUID(), "12345678000199", "Broker SA", "Broker", "ATIVA",
                    "CTVM", "01001000", "Rua A", "1", null, "Centro", "Sao Paulo", "SP", NOW), NOW);

    @Test
    void rejectsEveryMissingOrClosedPositionInputAndForeignAssociation() {
        PositionBalance open = new PositionBalance(PositionQuantity.positive(1),
                FinancialAmount.of("10"), FinancialAmount.of("10"));
        PositionBalance empty = PositionBalance.empty();

        assertThatThrownBy(() -> Position.create(null, account, association, asset, open))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.create(UUID.randomUUID(), null, association, asset, open))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.create(UUID.randomUUID(), account, null, asset, open))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.create(UUID.randomUUID(), account, association, null, open))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.create(UUID.randomUUID(), account, association, asset, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Position.create(UUID.randomUUID(), account, association, asset, empty))
                .isInstanceOf(IllegalArgumentException.class);

        Account other = Account.create(UUID.randomUUID(), "Other", "52998224725", "other@example.com",
                "hash", new BigDecimal("1000.00"), NOW);
        AccountBroker foreign = AccountBroker.create(UUID.randomUUID(), other, association.getBroker(), NOW);
        assertThatThrownBy(() -> Position.create(UUID.randomUUID(), account, foreign, asset, open))
                .isInstanceOf(IllegalArgumentException.class);

        Position position = Position.create(UUID.randomUUID(), account, association, asset, open);
        assertThatThrownBy(() -> position.apply(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
