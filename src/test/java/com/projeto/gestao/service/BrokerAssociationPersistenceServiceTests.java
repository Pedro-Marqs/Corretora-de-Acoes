package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.api.exception.BrokerRuleException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.PositionRepository;

class BrokerAssociationPersistenceServiceTests {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(
            Instant.parse("2026-08-27T12:00:00Z"), ZoneOffset.UTC);

    private AccountRepository accounts;
    private BrokerRepository brokers;
    private AccountBrokerRepository associations;
    private PositionRepository positions;
    private BrokerAssociationPersistenceService service;
    private Account account;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        brokers = mock(BrokerRepository.class);
        associations = mock(AccountBrokerRepository.class);
        positions = mock(PositionRepository.class);
        service = new BrokerAssociationPersistenceService(accounts, brokers, associations, positions,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        account = Account.create(ACCOUNT_ID, "Investidor", "12345678901", "investor@example.com",
                "hash", new BigDecimal("10000.00"), NOW);
        when(accounts.findByIdAndStatus(ACCOUNT_ID, com.projeto.gestao.domain.model.AccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        when(brokers.saveAndFlush(any(Broker.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(associations.saveAndFlush(any(AccountBroker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsSingleActiveAssociationForFirstRegistration() {
        when(brokers.findByCnpj(lookup().cnpj())).thenReturn(Optional.empty());
        when(associations.findByAccountIdAndBrokerId(any(), any())).thenReturn(Optional.empty());

        BrokerAssociationView result = service.associate(ACCOUNT_ID, lookup());

        assertThat(result.cnpj()).isEqualTo(lookup().cnpj());
        verify(brokers).saveAndFlush(any(Broker.class));
        verify(associations).saveAndFlush(any(AccountBroker.class));
    }

    @Test
    void rejectsAlreadyActiveAssociation() {
        Broker broker = broker();
        AccountBroker existing = AccountBroker.create(UUID.randomUUID(), account, broker, NOW);
        when(brokers.findByCnpj(lookup().cnpj())).thenReturn(Optional.of(broker));
        when(associations.findByAccountIdAndBrokerId(ACCOUNT_ID, broker.getId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.associate(ACCOUNT_ID, lookup()))
                .isInstanceOf(ConflictException.class);
        verify(associations, never()).saveAndFlush(any());
    }

    @Test
    void reactivatesSameHistoricalAssociation() {
        Broker broker = broker();
        AccountBroker existing = AccountBroker.create(UUID.randomUUID(), account, broker, NOW);
        UUID originalId = existing.getId();
        existing.inactivate(NOW.plusHours(1));
        when(brokers.findByCnpj(lookup().cnpj())).thenReturn(Optional.of(broker));
        when(associations.findByAccountIdAndBrokerId(ACCOUNT_ID, broker.getId()))
                .thenReturn(Optional.of(existing));

        BrokerAssociationView result = service.associate(ACCOUNT_ID, lookup());

        assertThat(result.associationId()).isEqualTo(originalId);
        assertThat(existing.getStatus()).isEqualTo(AssociationStatus.ACTIVE);
        assertThat(existing.getRemovedAt()).isNull();
    }

    @Test
    void updatesPresentFieldsAndPreservesMissingInstitutionalValues() {
        Broker broker = broker();
        BrokerLookup partial = new BrokerLookup(lookup().cnpj(), "Razão Atualizada", "", "ATIVA", "CTVM",
                "", "Rua Atualizada", null, "", "Cidade Atualizada", "SP");
        when(brokers.findByCnpj(partial.cnpj())).thenReturn(Optional.of(broker));
        when(associations.findByAccountIdAndBrokerId(ACCOUNT_ID, broker.getId())).thenReturn(Optional.empty());

        service.associate(ACCOUNT_ID, partial);

        assertThat(broker.getCorporateName()).isEqualTo("Razão Atualizada");
        assertThat(broker.getTradeName()).isEqualTo("XP");
        assertThat(broker.getPostalCode()).isEqualTo("22250911");
        assertThat(broker.getCity()).isEqualTo("Cidade Atualizada");
    }

    @Test
    void translatesConcurrentConstraintViolationToDuplicateConflict() {
        when(brokers.findByCnpj(lookup().cnpj())).thenReturn(Optional.empty());
        when(brokers.saveAndFlush(any(Broker.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent duplicate"));

        assertThatThrownBy(() -> service.associate(ACCOUNT_ID, lookup()))
                .isInstanceOf(ConflictException.class);
        verify(associations, never()).saveAndFlush(any());
    }

    @Test
    void listsOnlyActiveAssociationsFromAuthenticatedAccount() {
        Broker broker = broker();
        AccountBroker active = AccountBroker.create(UUID.randomUUID(), account, broker, NOW);
        when(associations.findByAccountIdAndStatus(ACCOUNT_ID, AssociationStatus.ACTIVE))
                .thenReturn(List.of(active));

        assertThat(service.listActive(ACCOUNT_ID))
                .extracting(BrokerAssociationView::associationId)
                .containsExactly(active.getId());
        verify(associations).findByAccountIdAndStatus(ACCOUNT_ID, AssociationStatus.ACTIVE);
    }

    @Test
    void removesAssociationWhenThereIsNoPositivePositionIncludingZeroQuantity() {
        Broker broker = broker();
        AccountBroker active = AccountBroker.create(UUID.randomUUID(), account, broker, NOW);
        when(associations.findForUpdateByIdAndAccountIdAndStatus(
                active.getId(), ACCOUNT_ID, AssociationStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(positions.existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
                ACCOUNT_ID, active.getId(), 0L)).thenReturn(false);

        service.remove(ACCOUNT_ID, active.getId());

        assertThat(active.getStatus()).isEqualTo(AssociationStatus.INACTIVE);
        assertThat(active.getRemovedAt()).isEqualTo(NOW);
    }

    @Test
    void blocksRemovalWhenAssociationHasPositivePosition() {
        Broker broker = broker();
        AccountBroker active = AccountBroker.create(UUID.randomUUID(), account, broker, NOW);
        when(associations.findForUpdateByIdAndAccountIdAndStatus(
                active.getId(), ACCOUNT_ID, AssociationStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(positions.existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
                ACCOUNT_ID, active.getId(), 0L)).thenReturn(true);

        assertThatThrownBy(() -> service.remove(ACCOUNT_ID, active.getId()))
                .isInstanceOf(BrokerRuleException.class);
        assertThat(active.getStatus()).isEqualTo(AssociationStatus.ACTIVE);
    }

    @Test
    void doesNotExposeOrModifyAssociationFromAnotherAccount() {
        UUID foreignAssociationId = UUID.randomUUID();
        when(associations.findForUpdateByIdAndAccountIdAndStatus(
                foreignAssociationId, ACCOUNT_ID, AssociationStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(ACCOUNT_ID, foreignAssociationId))
                .isInstanceOf(BrokerRuleException.class);
        verifyNoInteractions(positions);
    }

    private static BrokerLookup lookup() {
        return new BrokerLookup("02332886000104", "XP INVESTIMENTOS S/A", "XP", "ATIVA", "CTVM",
                "22250911", "Praia Botafogo", "", "Botafogo", "Rio de Janeiro", "RJ");
    }

    private static Broker broker() {
        return Broker.create(UUID.randomUUID(), lookup().cnpj(), "XP ANTIGA S/A", "XP", "ATIVA", "CTVM",
                "22250911", "Praia Botafogo", "S/N", null, "Botafogo", "Rio de Janeiro", "RJ", NOW);
    }
}
