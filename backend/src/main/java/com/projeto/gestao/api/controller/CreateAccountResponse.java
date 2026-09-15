package com.projeto.gestao.api.controller;

import java.math.BigDecimal;
import java.util.UUID;

import com.projeto.gestao.domain.model.AccountStatus;

public record CreateAccountResponse(
        UUID accountId, String name, BigDecimal balance, AccountStatus status) {
}
