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

    public static Movement initialBalance(
            UUID id, Account account, BigDecimal amount, OffsetDateTime occurredAt) {
        return base(id, account, MovementType.INITIAL_BALANCE, amount, Currency.BRL,
                occurredAt, amount);
    }

    public static Movement deposit(
            UUID id, Account account, BigDecimal amount, BigDecimal remainingBalance,
            OffsetDateTime occurredAt) {
        money(amount, "amount", false);
        return base(id, account, MovementType.DEPOSIT, amount, Currency.BRL,
                occurredAt, remainingBalance);
    }

    public static Movement purchase(
            UUID id, Account account, String ticker, Market market, BigDecimal quotePrice,
            long quantity, BigDecimal totalAmount, Currency currency, String brokerName,
            BigDecimal remainingBalance, OffsetDateTime occurredAt) {
        Movement movement = traded(id, account, MovementType.PURCHASE, ticker, market,
                quotePrice, quantity, totalAmount, currency, brokerName, remainingBalance,
                occurredAt);
        return movement;
    }

    public static Movement sale(
            UUID id, Account account, String ticker, Market market, BigDecimal quotePrice,
            long quantity, BigDecimal totalAmount, Currency currency, String brokerName,
            BigDecimal remainingBalance, BigDecimal realizedResult, OffsetDateTime occurredAt) {
        Movement movement = traded(id, account, MovementType.SALE, ticker, market,
                quotePrice, quantity, totalAmount, currency, brokerName, remainingBalance,
                occurredAt);
        movement.realizedResult = signedMoney(realizedResult, "realizedResult");
        return movement;
    }

    public static Movement transfer(
            UUID id, Account account, String ticker, Market market, long quantity,
            BigDecimal totalAmount, Currency currency, String originBrokerName,
            String destinationBrokerName, BigDecimal remainingBalance,
            OffsetDateTime occurredAt) {
        Movement movement = base(id, account, MovementType.TRANSFER, totalAmount, currency,
                occurredAt, remainingBalance);
        movement.ticker = text(ticker, "ticker");
        movement.market = required(market, "market");
        movement.quantity = positiveQuantity(quantity);
        movement.originBrokerName = text(originBrokerName, "originBrokerName");
        movement.destinationBrokerName = text(destinationBrokerName, "destinationBrokerName");
        if (movement.originBrokerName.equals(movement.destinationBrokerName)) {
            throw new IllegalArgumentException("Origin and destination brokers must differ");
        }
        validateMarketCurrency(market, currency);
        return movement;
    }

    private static Movement traded(
            UUID id, Account account, MovementType type, String ticker, Market market,
            BigDecimal quotePrice, long quantity, BigDecimal totalAmount, Currency currency,
            String brokerName, BigDecimal remainingBalance, OffsetDateTime occurredAt) {
        Movement movement = base(id, account, type, totalAmount, currency, occurredAt,
                remainingBalance);
        movement.ticker = text(ticker, "ticker");
        movement.market = required(market, "market");
        movement.quotePrice = money(quotePrice, "quotePrice", false);
        movement.quantity = positiveQuantity(quantity);
        movement.brokerName = text(brokerName, "brokerName");
        validateMarketCurrency(market, currency);
        return movement;
    }

    private static Movement base(
            UUID id, Account account, MovementType type, BigDecimal totalAmount,
            Currency currency, OffsetDateTime occurredAt, BigDecimal remainingBalance) {
        Movement movement = new Movement();
        movement.id = required(id, "id");
        movement.account = required(account, "account");
        movement.movementType = required(type, "movementType");
        movement.totalAmount = money(totalAmount, "totalAmount", false);
        movement.currency = required(currency, "currency");
        movement.occurredAt = required(occurredAt, "occurredAt");
        movement.remainingBalance = money(remainingBalance, "remainingBalance", true);
        return movement;
    }

    private static BigDecimal money(BigDecimal value, String field, boolean allowZero) {
        required(value, field);
        BigDecimal normalized = value.setScale(2, java.math.RoundingMode.HALF_UP);
        if (allowZero ? value.signum() < 0 : value.signum() <= 0 || normalized.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be "
                    + (allowZero ? "non-negative" : "positive"));
        }
        return normalized;
    }

    private static BigDecimal signedMoney(BigDecimal value, String field) {
        required(value, field);
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static Long positiveQuantity(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return value;
    }

    private static String text(String value, String field) {
        required(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static void validateMarketCurrency(Market market, Currency currency) {
        if ((market == Market.BR && currency != Currency.BRL)
                || (market == Market.US && currency != Currency.USD)) {
            throw new IllegalArgumentException("currency is inconsistent with market");
        }
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

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
