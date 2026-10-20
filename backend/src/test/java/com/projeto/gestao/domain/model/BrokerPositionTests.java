package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BrokerPositionTests {
    @Test
    void rejectsBlankBrokerIdentifiersAndMissingBalances() {
        assertThatThrownBy(() -> new BrokerPosition(null, PositionBalance.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BrokerPosition(" ", PositionBalance.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BrokerPosition("broker", null))
                .isInstanceOf(NullPointerException.class);
    }
}
