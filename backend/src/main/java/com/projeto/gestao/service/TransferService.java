package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.BusinessRuleException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.AssetStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.BrokerPosition;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.PositionBalance;
import com.projeto.gestao.domain.model.PositionFinancialCalculator;
import com.projeto.gestao.domain.model.PositionQuantity;
import com.projeto.gestao.domain.model.PositionTransferResult;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountRepository accounts;
    private final AccountBrokerRepository associations;
    private final AssetRepository assets;
    private final PositionRepository positions;
    private final FinancialHistoryService history;
    private final Clock clock;

    public TransferService(AccountRepository accounts, AccountBrokerRepository associations,
            AssetRepository assets, PositionRepository positions,
            FinancialHistoryService history, Clock clock) {
        this.accounts = accounts;
        this.associations = associations;
        this.assets = assets;
        this.positions = positions;
        this.history = history;
        this.clock = clock;
    }

    @Transactional
    public TransferResult transfer(UUID accountId, UUID originId, UUID destinationId,
            UUID assetId, long quantity) {
        if (accountId == null) throw new AuthenticationException();
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (originId == null || destinationId == null || originId.equals(destinationId)) {
            throw new AuthorizationException();
        }

        Account account = accounts.findForUpdateByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
        Asset asset = assets.findByIdAndStatus(assetId, AssetStatus.ACTIVE)
                .orElseThrow(() -> new MarketDataUnavailableException("Ativo indisponível."));

        List<AccountBroker> lockedAssociations = associations.findAllForUpdateOrdered(
                accountId, List.of(originId, destinationId));
        if (lockedAssociations.size() != 2 || lockedAssociations.stream()
                .anyMatch(item -> item.getStatus() != AssociationStatus.ACTIVE)) {
            throw new AuthorizationException();
        }
        AccountBroker originAssociation = association(lockedAssociations, originId);
        AccountBroker destinationAssociation = association(lockedAssociations, destinationId);

        List<Position> lockedPositions = positions.findAllForUpdateOrdered(
                accountId, asset.getId(), List.of(originId, destinationId));
        Position origin = position(lockedPositions, originId);
        Position destination = position(lockedPositions, destinationId);
        long available = origin == null ? 0 : origin.getQuantity();
        if (quantity > available) {
            throw BusinessRuleException.insufficientPosition(quantity, available);
        }

        PositionBalance destinationBalance = destination == null
                ? PositionBalance.empty() : destination.financialBalance();
        PositionTransferResult financial = PositionFinancialCalculator.transfer(
                new BrokerPosition(originId.toString(), origin.financialBalance()),
                new BrokerPosition(destinationId.toString(), destinationBalance),
                PositionQuantity.positive(quantity));
        origin.apply(financial.origin().balance());
        if (destination == null) {
            destination = positions.save(Position.create(UUID.randomUUID(), account,
                    destinationAssociation, asset, financial.destination().balance()));
        } else {
            destination.apply(financial.destination().balance());
            destination = positions.save(destination);
        }

        OffsetDateTime occurredAt = OffsetDateTime.now(clock);
        history.record(account, occurredAt, (id, owner, instant) -> Movement.transfer(
                id, owner, asset.getTicker(), asset.getMarket(), quantity,
                financial.transferredCost().value(), asset.getCurrency(),
                originAssociation.getBroker().getTradeName(),
                destinationAssociation.getBroker().getTradeName(), account.getBalance(), instant));

        return new TransferResult(asset.getId(), originId, destinationId, asset.getTicker(),
                asset.getMarket(), asset.getCurrency(), quantity, origin.getQuantity(),
                origin.getAveragePrice(), origin.getTotalCost(), destination.getQuantity(),
                destination.getAveragePrice(), destination.getTotalCost(),
                financial.transferredCost().value(), account.getBalance(), occurredAt);
    }

    private AccountBroker association(List<AccountBroker> locked, UUID id) {
        return locked.stream().filter(item -> item.getId().equals(id)).findFirst()
                .orElseThrow(AuthorizationException::new);
    }

    private Position position(List<Position> locked, UUID associationId) {
        return locked.stream().filter(item -> item.getAccountBroker().getId().equals(associationId))
                .findFirst().orElse(null);
    }
}
