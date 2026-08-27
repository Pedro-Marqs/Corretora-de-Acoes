package com.projeto.gestao.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.Broker;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BrokerRepositoryTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-27T10:00:00-03:00");

    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private PositionRepository positions;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void locatesInactiveHistoricalAssociationAndDetectsOnlyPositivePosition() {
        String uniqueCpf = String.format("%011d", ThreadLocalRandom.current().nextLong(100_000_000_000L));
        Account account = accounts.saveAndFlush(Account.create(UUID.randomUUID(), "Investidor",
                uniqueCpf, UUID.randomUUID() + "@broker-repository.example", "hash",
                new BigDecimal("10000.00"), NOW));
        Broker broker = brokers.saveAndFlush(Broker.create(UUID.randomUUID(), "02332886000104",
                "XP INVESTIMENTOS S/A", "XP", "ATIVA", "CTVM", "22250911", "Praia Botafogo",
                "S/N", null, "Botafogo", "Rio de Janeiro", "RJ", NOW));
        AccountBroker association = AccountBroker.create(UUID.randomUUID(), account, broker, NOW);
        association.inactivate(NOW.plusHours(1));
        associations.saveAndFlush(association);

        assertThat(associations.findByAccountIdAndBrokerId(account.getId(), broker.getId()))
                .get().extracting(AccountBroker::getStatus).isEqualTo(AssociationStatus.INACTIVE);
        assertThat(associations.findForUpdateByIdAndAccountIdAndStatus(
                association.getId(), account.getId(), AssociationStatus.INACTIVE)).isPresent();

        UUID assetId = UUID.randomUUID();
        jdbc.update("INSERT INTO asset (id,ticker,name,market,currency) VALUES (?,?,?,'BR','BRL')",
                assetId, "T19T3", "Ativo T19");
        jdbc.update("""
                INSERT INTO position
                    (id,account_id,account_broker_id,asset_id,quantity,average_price,total_cost)
                VALUES (?,?,?,?,0,0.00,0.00)
                """, UUID.randomUUID(), account.getId(), association.getId(), assetId);

        assertThat(positions.existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
                account.getId(), association.getId(), 0L)).isFalse();
        jdbc.update("UPDATE position SET quantity=1,total_cost=10.00,average_price=10.00 WHERE asset_id=?",
                assetId);
        assertThat(positions.existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
                account.getId(), association.getId(), 0L)).isTrue();
    }
}
