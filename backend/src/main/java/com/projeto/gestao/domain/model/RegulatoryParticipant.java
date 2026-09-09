package com.projeto.gestao.domain.model;

/** Registro interno de uma categoria atribuída a um participante pela CVM. */
public record RegulatoryParticipant(
        String category,
        String legalName,
        String tradeName,
        String status,
        String cvmCode) {
}
