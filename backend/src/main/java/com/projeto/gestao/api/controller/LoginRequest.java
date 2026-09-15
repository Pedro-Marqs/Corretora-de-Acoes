package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,
        @NotBlank(message = "Senha é obrigatória.")
        String password) {
    public LoginRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
