package com.projeto.gestao.domain.model;

import java.util.Objects;

public record BrokerPosition(String brokerId, PositionBalance balance) {
    public BrokerPosition {
        if (brokerId == null || brokerId.isBlank()) {
            throw new IllegalArgumentException("brokerId must not be blank");
        }
        Objects.requireNonNull(balance, "balance must not be null");
    }
}
