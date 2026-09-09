package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,
        @NotBlank(message = "Senha é obrigatória.") String password,
        @NotBlank(message = "Confirmação é obrigatória.")
        @Pattern(regexp = "Excluir", message = "Confirmação deve ser exatamente Excluir.")
        String confirmation) {
    public DeleteAccountRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "DeleteAccountRequest[redacted]";
    }
}
