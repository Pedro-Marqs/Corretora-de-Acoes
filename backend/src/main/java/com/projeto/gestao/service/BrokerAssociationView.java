package com.projeto.gestao.service;

import java.util.UUID;

public record BrokerAssociationView(
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
}
