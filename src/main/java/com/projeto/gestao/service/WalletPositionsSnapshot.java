package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.util.List;

public record WalletPositionsSnapshot(BigDecimal availableBalance, List<WalletPositionView> positions) { }
