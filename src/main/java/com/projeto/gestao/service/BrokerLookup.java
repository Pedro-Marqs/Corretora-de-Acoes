package com.projeto.gestao.service;

public record BrokerLookup(
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
