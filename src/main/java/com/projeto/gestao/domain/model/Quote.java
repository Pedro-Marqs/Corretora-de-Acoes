package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quote")
public class Quote {
    @Id @Column(name = "asset_id") private java.util.UUID assetId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "asset_id") private Asset asset;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal price;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 3) private Currency currency;
    @Column(name = "quoted_at", nullable = false) private OffsetDateTime quotedAt;
    @Column(name = "collected_at", nullable = false) private OffsetDateTime collectedAt;

    protected Quote() { }

    public java.util.UUID getAssetId() { return assetId; }
    public Asset getAsset() { return asset; }
    public BigDecimal getPrice() { return price; }
    public Currency getCurrency() { return currency; }
    public OffsetDateTime getQuotedAt() { return quotedAt; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
}
