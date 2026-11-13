package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.NotNull;

public record AdminInvestorStatusRequest(
        @NotNull(message = "Status é obrigatório.") Status status) {
    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
