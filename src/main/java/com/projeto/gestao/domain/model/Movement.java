package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "movement")
public class Movement {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private Account account;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(name = "movement_type", nullable = false, length = 24) private MovementType movementType;
    @Column(length = 20) private String ticker;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(length = 8) private Market market;
    @Column(name = "quote_price", precision = 19, scale = 2) private BigDecimal quotePrice;
    private Long quantity;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2) private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable = false, length = 3) private Currency currency;
    @Column(name = "broker_name", length = 200) private String brokerName;
    @Column(name = "origin_broker_name", length = 200) private String originBrokerName;
    @Column(name = "destination_broker_name", length = 200) private String destinationBrokerName;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2) private BigDecimal remainingBalance;
    @Column(name = "realized_result", precision = 19, scale = 2) private BigDecimal realizedResult;

    protected Movement() { }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public MovementType getMovementType() { return movementType; }
    public String getTicker() { return ticker; }
    public Market getMarket() { return market; }
    public BigDecimal getQuotePrice() { return quotePrice; }
    public Long getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Currency getCurrency() { return currency; }
    public String getBrokerName() { return brokerName; }
    public String getOriginBrokerName() { return originBrokerName; }
    public String getDestinationBrokerName() { return destinationBrokerName; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public BigDecimal getRealizedResult() { return realizedResult; }
}
