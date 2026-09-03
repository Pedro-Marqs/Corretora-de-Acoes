package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "position", uniqueConstraints = @UniqueConstraint(name = "uq_position_account_broker_asset", columnNames = {"account_id", "account_broker_id", "asset_id"}))
public class Position {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private Account account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_broker_id") private AccountBroker accountBroker;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "asset_id") private Asset asset;
    @Column(nullable = false) private long quantity;
    @Column(name = "average_price", nullable = false, precision = 19, scale = 2) private BigDecimal averagePrice;
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 2) private BigDecimal totalCost;

    protected Position() { }

    public static Position create(UUID id, Account account, AccountBroker accountBroker,
            Asset asset, PositionBalance balance) {
        if (id == null || account == null || accountBroker == null || asset == null
                || balance == null || !balance.isOpen()) {
            throw new IllegalArgumentException("Open position data is required");
        }
        if (accountBroker.getAccount() != account) {
            throw new IllegalArgumentException("Broker association must belong to account");
        }
        Position position = new Position();
        position.id = id;
        position.account = account;
        position.accountBroker = accountBroker;
        position.asset = asset;
        position.apply(balance);
        return position;
    }

    public PositionBalance financialBalance() {
        return new PositionBalance(new PositionQuantity(quantity),
                new FinancialAmount(totalCost), new FinancialAmount(averagePrice));
    }

    public void apply(PositionBalance balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Position balance is required");
        }
        quantity = balance.quantity().value();
        averagePrice = balance.averagePrice().value();
        totalCost = balance.totalCost().value();
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public AccountBroker getAccountBroker() { return accountBroker; }
    public Asset getAsset() { return asset; }
    public long getQuantity() { return quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public BigDecimal getTotalCost() { return totalCost; }
}
