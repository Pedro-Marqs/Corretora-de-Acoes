package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "exchange_rate")
public class ExchangeRate implements Persistable<String> {
    @Id @Column(name = "currency_pair", length = 7) private String currencyPair;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal rate;
    @Column(name = "quoted_at", nullable = false) private OffsetDateTime quotedAt;
    @Column(name = "collected_at", nullable = false) private OffsetDateTime collectedAt;
    @Column(nullable = false, length = 60) private String source;
    @Transient private boolean newEntity = true;

    protected ExchangeRate() { }

    public ExchangeRate(String currencyPair, BigDecimal rate, OffsetDateTime quotedAt,
            OffsetDateTime collectedAt, String source) {
        this.currencyPair = currencyPair;
        replace(rate, quotedAt, collectedAt, source);
    }

    public void replace(BigDecimal rate, OffsetDateTime quotedAt, OffsetDateTime collectedAt,
            String source) {
        this.rate = rate;
        this.quotedAt = quotedAt;
        this.collectedAt = collectedAt;
        this.source = source;
    }

    public String getCurrencyPair() { return currencyPair; }
    @Override public String getId() { return currencyPair; }
    @Override public boolean isNew() { return newEntity; }
    @PostLoad @PostPersist void markNotNew() { newEntity = false; }
    public BigDecimal getRate() { return rate; }
    public OffsetDateTime getQuotedAt() { return quotedAt; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
    public String getSource() { return source; }
}
