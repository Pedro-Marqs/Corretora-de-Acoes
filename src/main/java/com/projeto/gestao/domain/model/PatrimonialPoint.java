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
    @Column(name = "patrimony_brl", nullable = false, precision = 19, scale = 2) private BigDecimal patrimonyBrl;

    protected PatrimonialPoint() { }

    public static PatrimonialPoint initial(
            UUID id, Account account, Movement movement,
            BigDecimal patrimonyBrl, OffsetDateTime recordedAt) {
        PatrimonialPoint point = new PatrimonialPoint();
        point.id = id;
        point.account = account;
        point.movement = movement;
        point.patrimonyBrl = patrimonyBrl;
        point.recordedAt = recordedAt;
        return point;
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public Movement getMovement() { return movement; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public BigDecimal getPatrimonyBrl() { return patrimonyBrl; }
}
