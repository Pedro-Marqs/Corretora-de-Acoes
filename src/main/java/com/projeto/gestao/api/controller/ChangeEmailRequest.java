package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank(message = "Novo e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String newEmail,
        @NotBlank(message = "Senha atual é obrigatória.")
        String currentPassword) {
    public ChangeEmailRequest {
        newEmail = newEmail == null ? null : newEmail.trim();
    }

    @Override
    public String toString() {
        return "ChangeEmailRequest[redacted]";
    }
}
