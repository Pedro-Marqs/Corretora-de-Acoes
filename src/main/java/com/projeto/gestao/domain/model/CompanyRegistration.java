package com.projeto.gestao.domain.model;

/** Dados cadastrais internos de uma empresa, independentes do provedor externo. */
public record CompanyRegistration(
        String cnpj,
        String legalName,
        String tradeName,
        String registrationStatus,
        String postalCode) {
}
