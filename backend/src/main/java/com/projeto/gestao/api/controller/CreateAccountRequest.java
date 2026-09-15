package com.projeto.gestao.api.controller;

import com.projeto.gestao.api.validation.ValidCpf;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String name,
        @NotBlank(message = "CPF é obrigatório.") @ValidCpf String cpf,
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres.")
        String email,
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres.")
        @Pattern(regexp = ".*[a-z].*", message = "Senha deve conter letra minúscula.")
        @Pattern(regexp = ".*[A-Z].*", message = "Senha deve conter letra maiúscula.")
        @Pattern(regexp = ".*\\d.*", message = "Senha deve conter número.")
        @Pattern(regexp = ".*[^A-Za-z0-9\\s].*", message = "Senha deve conter caractere especial.")
        String password) {
    public CreateAccountRequest {
        name = name == null ? null : name.trim();
        cpf = cpf == null ? null : cpf.trim();
        email = email == null ? null : email.trim();
    }
}
