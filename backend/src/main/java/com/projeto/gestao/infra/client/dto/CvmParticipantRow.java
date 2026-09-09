package com.projeto.gestao.infra.client.dto;

public record CvmParticipantRow(
        String category,
        String cnpj,
        String legalName,
        String tradeName,
        String status,
        String cvmCode) {
}
