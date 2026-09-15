package com.projeto.gestao.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "patrimonial_point")
public class PatrimonialPoint {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "account_id") private Account account;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "movement_id", unique = true) private Movement movement;
    @Column(name = "recorded_at", nullable = false) private OffsetDateTime recordedAt;
    @Column(name = "balance_brl", nullable = false, precision = 19, scale = 2) private BigDecimal balanceBrl;
    @Column(name = "positions_value_brl", nullable = false, precision = 19, scale = 2) private BigDecimal positionsValueBrl;
    @Column(name = "usd_brl_rate", precision = 19, scale = 2) private BigDecimal usdBrlRate;
    @Column(name = "patrimony_brl", nullable = false, precision = 19, scale = 2) private BigDecimal patrimonyBrl;

    protected PatrimonialPoint() { }

    public static PatrimonialPoint initial(
            UUID id, Account account, Movement movement,
            BigDecimal patrimonyBrl, OffsetDateTime recordedAt) {
        return create(id, account, movement, patrimonyBrl, BigDecimal.ZERO,
                null, patrimonyBrl, recordedAt);
    }

    public static PatrimonialPoint create(
            UUID id, Account account, Movement movement,
            BigDecimal balanceBrl, BigDecimal positionsValueBrl, BigDecimal usdBrlRate,
            BigDecimal patrimonyBrl, OffsetDateTime recordedAt) {
        PatrimonialPoint point = new PatrimonialPoint();
        point.id = required(id, "id");
        point.account = required(account, "account");
        point.movement = required(movement, "movement");
        if (movement.getAccount() != account) {
            throw new IllegalArgumentException("movement must belong to account");
        }
        point.balanceBrl = money(balanceBrl, "balanceBrl", false);
        point.positionsValueBrl = money(positionsValueBrl, "positionsValueBrl", false);
        point.usdBrlRate = usdBrlRate == null ? null : money(usdBrlRate, "usdBrlRate", true);
        point.patrimonyBrl = money(patrimonyBrl, "patrimonyBrl", false);
        if (point.balanceBrl.add(point.positionsValueBrl).compareTo(point.patrimonyBrl) != 0) {
            throw new IllegalArgumentException("patrimonyBrl must equal balance plus positions");
        }
        point.recordedAt = required(recordedAt, "recordedAt");
        return point;
    }

    private static BigDecimal money(BigDecimal value, String field, boolean positive) {
        required(value, field);
        if (positive ? value.signum() <= 0 : value.signum() < 0) {
            throw new IllegalArgumentException(field + " must be " + (positive ? "positive" : "non-negative"));
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static <T> T required(T value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public Movement getMovement() { return movement; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public BigDecimal getBalanceBrl() { return balanceBrl; }
    public BigDecimal getPositionsValueBrl() { return positionsValueBrl; }
    public BigDecimal getUsdBrlRate() { return usdBrlRate; }
    public BigDecimal getPatrimonyBrl() { return patrimonyBrl; }
}
