package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.BrokerRuleException;
import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.PositionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrokerAssociationPersistenceService {

    private static final String UNKNOWN_NUMBER = "S/N";

    private final AccountRepository accounts;
    private final BrokerRepository brokers;
    private final AccountBrokerRepository associations;
    private final PositionRepository positions;
    private final Clock clock;

    public BrokerAssociationPersistenceService(AccountRepository accounts, BrokerRepository brokers,
            AccountBrokerRepository associations, PositionRepository positions, Clock clock) {
        this.accounts = accounts;
        this.brokers = brokers;
        this.associations = associations;
        this.positions = positions;
        this.clock = clock;
    }

    @Transactional
    public BrokerAssociationView associate(UUID accountId, BrokerLookup lookup) {
        Account account = activeAccount(accountId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        Optional<Broker> knownBroker = brokers.findByCnpj(lookup.cnpj());
        Broker broker = knownBroker.orElseGet(() -> createBroker(lookup, now));
        if (knownBroker.isPresent()) {
            merge(broker, lookup, now);
        }
        Broker persistedBroker;
        try {
            persistedBroker = brokers.saveAndFlush(broker);
        } catch (DataIntegrityViolationException exception) {
            throw ConflictException.brokerAlreadyRegistered();
        }

        AccountBroker association = associations.findByAccountIdAndBrokerId(accountId, persistedBroker.getId())
                .map(existing -> reactivateOrReject(existing))
                .orElseGet(() -> AccountBroker.create(UUID.randomUUID(), account, persistedBroker, now));
        try {
            return view(associations.saveAndFlush(association));
        } catch (DataIntegrityViolationException exception) {
            throw ConflictException.brokerAlreadyRegistered();
        }
    }

    @Transactional(readOnly = true)
    public List<BrokerAssociationView> listActive(UUID accountId) {
        activeAccount(accountId);
        return associations.findByAccountIdAndStatus(accountId, AssociationStatus.ACTIVE).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public void remove(UUID accountId, UUID associationId) {
        activeAccount(accountId);
        AccountBroker association = associations
                .findForUpdateByIdAndAccountIdAndStatus(associationId, accountId, AssociationStatus.ACTIVE)
                .orElseThrow(BrokerRuleException::associationNotFound);
        if (positions.existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
                accountId, associationId, 0L)) {
            throw BrokerRuleException.openPosition();
        }
        association.inactivate(OffsetDateTime.now(clock));
    }

    private Account activeAccount(UUID accountId) {
        if (accountId == null) {
            throw new AuthenticationException();
        }
        return accounts.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
    }

    private Broker createBroker(BrokerLookup value, OffsetDateTime now) {
        return Broker.create(UUID.randomUUID(), value.cnpj(), value.corporateName(), value.tradeName(),
                value.registrationStatus(), value.cvmCategory(), value.postalCode(), value.street(),
                UNKNOWN_NUMBER, value.complement(), value.district(), value.city(), value.state(), now);
    }

    private void merge(Broker broker, BrokerLookup value, OffsetDateTime now) {
        broker.mergeInstitutionalData(value.corporateName(), value.tradeName(), value.registrationStatus(),
                value.cvmCategory(), value.postalCode(), value.street(), null,
                value.complement(), value.district(), value.city(), value.state(), now);
    }

    private AccountBroker reactivateOrReject(AccountBroker association) {
        if (association.getStatus() == AssociationStatus.ACTIVE) {
            throw ConflictException.brokerAlreadyRegistered();
        }
        association.reactivate();
        return association;
    }

    private BrokerAssociationView view(AccountBroker association) {
        Broker broker = association.getBroker();
        return new BrokerAssociationView(association.getId(), broker.getCnpj(), broker.getCorporateName(),
                broker.getTradeName(), broker.getRegistrationStatus(), broker.getCvmCategory(),
                broker.getPostalCode(), broker.getStreet(), broker.getComplement(), broker.getDistrict(),
                broker.getCity(), broker.getState());
    }
}
