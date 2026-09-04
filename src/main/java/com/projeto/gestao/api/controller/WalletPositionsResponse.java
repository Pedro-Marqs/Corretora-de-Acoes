package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.util.List;

import com.projeto.gestao.service.WalletPositionView;
import com.projeto.gestao.service.WalletPositionsSnapshot;

public record WalletPositionsResponse(BigDecimal availableBalance, List<WalletPositionView> positions) {
    static WalletPositionsResponse from(WalletPositionsSnapshot snapshot) {
        return new WalletPositionsResponse(snapshot.availableBalance(), snapshot.positions());
    }
}
