package com.projeto.gestao.api.controller;

import com.projeto.gestao.service.BrokerLookup;

public record BrokerLookupResponse(
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

    static BrokerLookupResponse from(BrokerLookup value) {
        return new BrokerLookupResponse(value.cnpj(), value.corporateName(), value.tradeName(),
                value.registrationStatus(), value.cvmCategory(), value.postalCode(), value.street(),
                value.complement(), value.district(), value.city(), value.state());
    }
}
