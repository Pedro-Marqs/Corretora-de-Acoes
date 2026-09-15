package com.projeto.gestao.api.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Senha atual é obrigatória.")
        String currentPassword,
        @NotBlank(message = "Nova senha é obrigatória.")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres.")
        @Pattern(regexp = ".*[a-z].*", message = "Senha deve conter letra minúscula.")
        @Pattern(regexp = ".*[A-Z].*", message = "Senha deve conter letra maiúscula.")
        @Pattern(regexp = ".*\\d.*", message = "Senha deve conter número.")
        @Pattern(regexp = ".*[^A-Za-z0-9\\s].*", message = "Senha deve conter caractere especial.")
        String newPassword) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[redacted]";
    }
}
