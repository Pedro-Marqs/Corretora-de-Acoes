package com.projeto.gestao.api.controller;

import com.projeto.gestao.api.validation.ValidCpf;
import jakarta.validation.constraints.NotBlank;

public record ReactivationRequest(
        @NotBlank(message = "CPF é obrigatório.") @ValidCpf String cpf) {
    public ReactivationRequest {
        cpf = cpf == null ? null : cpf.trim();
    }
}
