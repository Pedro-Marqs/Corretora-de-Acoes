package com.projeto.gestao.domain.model;

import java.util.List;

/** Situação de um CNPJ no cadastro oficial de participantes da CVM. */
public record RegulatoryRegistration(
        String cnpj,
        boolean registered,
        boolean activeCtvm,
        List<RegulatoryParticipant> participants) {

    public RegulatoryRegistration {
        participants = List.copyOf(participants);
    }
}
