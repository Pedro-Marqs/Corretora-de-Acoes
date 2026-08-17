package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {
    @Id @Column(name = "currency_pair", length = 7) private String currencyPair;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal rate;
    @Column(name = "quoted_at", nullable = false) private OffsetDateTime quotedAt;
    @Column(name = "collected_at", nullable = false) private OffsetDateTime collectedAt;

    protected ExchangeRate() { }

    public String getCurrencyPair() { return currencyPair; }
    public BigDecimal getRate() { return rate; }
    public OffsetDateTime getQuotedAt() { return quotedAt; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
}
