package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class BrokerAssociationTests {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 8, 27, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void createsInactivatesAndReactivatesSameAssociation() {
        AccountBroker association = AccountBroker.create(UUID.randomUUID(), account(), broker(), NOW);
        UUID originalId = association.getId();

        association.inactivate(NOW.plusHours(1));
        assertThat(association.getStatus()).isEqualTo(AssociationStatus.INACTIVE);
        assertThat(association.getRemovedAt()).isNotNull();

        association.reactivate();
        assertThat(association.getId()).isEqualTo(originalId);
        assertThat(association.getStatus()).isEqualTo(AssociationStatus.ACTIVE);
        assertThat(association.getRemovedAt()).isNull();
    }

    @Test
    void brokerMergePreservesMissingValuesAndNeverChangesCnpj() {
        Broker broker = broker();
        broker.mergeInstitutionalData("Nova Razão", "", "ATIVA", "CTVM", "", "Nova Rua",
                "", null, "", "Nova Cidade", "SP", NOW.plusDays(1));

        assertThat(broker.getCnpj()).isEqualTo("02332886000104");
        assertThat(broker.getCorporateName()).isEqualTo("Nova Razão");
        assertThat(broker.getTradeName()).isEqualTo("XP");
        assertThat(broker.getPostalCode()).isEqualTo("22250911");
        assertThat(broker.getNumber()).isEqualTo("S/N");
        assertThat(broker.getCity()).isEqualTo("Nova Cidade");
    }

    @Test
    void unchangedPartialMergePreservesTimestampAndRealStreetNumber() {
        Broker broker = Broker.create(UUID.randomUUID(), "02332886000104", "XP INVESTIMENTOS S/A", "XP",
                "ATIVA", "CTVM", "22250911", "Praia Botafogo", "123", null,
                "Botafogo", "Rio de Janeiro", "RJ", NOW);

        broker.mergeInstitutionalData(null, "", null, null, "", null, null, null,
                "", null, null, NOW.plusDays(1));

        assertThat(broker.getNumber()).isEqualTo("123");
        assertThat(broker.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsInvalidStateTransitions() {
        AccountBroker association = AccountBroker.create(UUID.randomUUID(), account(), broker(), NOW);
        assertThatThrownBy(association::reactivate).isInstanceOf(IllegalStateException.class);
        association.inactivate(NOW);
        assertThatThrownBy(() -> association.inactivate(NOW)).isInstanceOf(IllegalStateException.class);
    }

    private static Account account() {
        return Account.create(UUID.randomUUID(), "Investidor", "12345678901", "investor@example.com",
                "hash", new BigDecimal("10000.00"), NOW);
    }

    private static Broker broker() {
        return Broker.create(UUID.randomUUID(), "02332886000104", "XP INVESTIMENTOS S/A", "XP",
                "ATIVA", "CTVM", "22250911", "Praia Botafogo", "S/N", null,
                "Botafogo", "Rio de Janeiro", "RJ", NOW);
    }
}
