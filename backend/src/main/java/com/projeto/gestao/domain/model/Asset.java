package com.projeto.gestao.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "asset")
public class Asset {
    @Id private UUID id;
    @Column(nullable = false, length = 20) private String ticker;
    @Column(nullable = false, length = 200) private String name;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 8) private Market market;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 3) private Currency currency;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "asset_type", nullable = false, length = 20) private AssetType type;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 16) private AssetStatus status;

    protected Asset() { }

    public Asset(String ticker, String name, Market market, Currency currency) {
        this.id = UUID.randomUUID();
        this.ticker = ticker;
        this.name = name;
        this.market = market;
        this.currency = currency;
        this.type = AssetType.STOCK;
        this.status = AssetStatus.ACTIVE;
    }

    public void updateCatalog(String name) {
        this.name = name;
        this.status = AssetStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public Market getMarket() { return market; }
    public Currency getCurrency() { return currency; }
    public AssetType getType() { return type; }
    public AssetStatus getStatus() { return status; }
}
