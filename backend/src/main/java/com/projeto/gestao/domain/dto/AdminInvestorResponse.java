package com.projeto.gestao.domain.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminInvestorResponse(
        UUID id,
        String name,
        String emailMasked,
        String cpfMasked,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime inactivatedAt) {

    public static AdminInvestorResponse from(AdminInvestorProjection account) {
        return new AdminInvestorResponse(
                account.getId(), account.getName(), maskEmail(account.getEmail()),
                maskCpf(account.getCpf()), account.getStatus().name(), account.getCreatedAt(),
                account.getInactivatedAt());
    }

    private static String maskEmail(String email) {
        int separator = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(separator);
    }

    private static String maskCpf(String cpf) {
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
