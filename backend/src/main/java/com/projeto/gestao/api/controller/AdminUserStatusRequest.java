package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(
        @NotNull(message = "Status is required.") Status status) {
    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
