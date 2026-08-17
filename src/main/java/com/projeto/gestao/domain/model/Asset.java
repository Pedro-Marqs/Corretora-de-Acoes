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

    protected Asset() { }

    public UUID getId() { return id; }
    public String getTicker() { return ticker; }
    public String getName() { return name; }
    public Market getMarket() { return market; }
    public Currency getCurrency() { return currency; }
}
