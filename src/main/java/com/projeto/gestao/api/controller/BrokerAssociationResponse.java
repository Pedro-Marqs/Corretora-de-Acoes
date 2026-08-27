package com.projeto.gestao.api.controller;

import java.util.UUID;

import com.projeto.gestao.service.BrokerAssociationView;

public record BrokerAssociationResponse(
        UUID associationId,
        String cnpj,
        String corporateName,
        String tradeName,
        String registrationStatus,
        String cvmCategory,
        String postalCode,
        String street,
        String complement,
        String district,
        String city,
        String state) {

    static BrokerAssociationResponse from(BrokerAssociationView value) {
        return new BrokerAssociationResponse(value.associationId(), value.cnpj(), value.corporateName(),
                value.tradeName(), value.registrationStatus(), value.cvmCategory(), value.postalCode(),
                value.street(), value.complement(), value.district(), value.city(), value.state());
    }
}
