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
    @Column(nullable = false) private boolean stale;
    @Transient private boolean newEntity = true;

    protected ExchangeRate() { }

    public ExchangeRate(String currencyPair, BigDecimal rate, OffsetDateTime quotedAt,
            OffsetDateTime collectedAt, String source) {
        this.currencyPair = currencyPair;
        applySnapshot(rate, quotedAt, collectedAt, source);
    }

    public void replace(BigDecimal rate, OffsetDateTime quotedAt, OffsetDateTime collectedAt,
            String source) {
        applySnapshot(rate, quotedAt, collectedAt, source);
    }

    private void applySnapshot(BigDecimal rate, OffsetDateTime quotedAt,
            OffsetDateTime collectedAt, String source) {
        this.rate = new FinancialAmount(rate).value();
        this.quotedAt = quotedAt;
        this.collectedAt = collectedAt;
        this.source = source;
        this.stale = false;
    }

    public void markStale() { this.stale = true; }
    public void clearStale() { this.stale = false; }

    public String getCurrencyPair() { return currencyPair; }
    @Override public String getId() { return currencyPair; }
    @Override public boolean isNew() { return newEntity; }
    @PostLoad @PostPersist void markNotNew() { newEntity = false; }
    public BigDecimal getRate() { return rate; }
    public OffsetDateTime getQuotedAt() { return quotedAt; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
    public String getSource() { return source; }
    public boolean isStale() { return stale; }
}
