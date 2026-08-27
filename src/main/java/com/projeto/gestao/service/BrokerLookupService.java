package com.projeto.gestao.service;

import com.projeto.gestao.api.exception.BrokerRuleException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.CompanyRegistration;
import com.projeto.gestao.domain.model.PostalAddress;
import com.projeto.gestao.domain.model.RegulatoryRegistration;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.PostalAddressPort;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;
import com.projeto.gestao.infra.adapter.ExternalIdentifierNormalizer;
import org.springframework.stereotype.Service;

@Service
public class BrokerLookupService {

    private static final String ACTIVE_STATUS = "ATIVA";

    private final CompanyRegistryPort companies;
    private final PostalAddressPort addresses;
    private final RegulatoryRegistryPort regulatoryRegistry;

    public BrokerLookupService(CompanyRegistryPort companies, PostalAddressPort addresses,
            RegulatoryRegistryPort regulatoryRegistry) {
        this.companies = companies;
        this.addresses = addresses;
        this.regulatoryRegistry = regulatoryRegistry;
    }

    public BrokerLookup lookup(String cnpj) {
        try {
            String normalized = ExternalIdentifierNormalizer.cnpj(cnpj, "BrokerLookup");
            CompanyRegistration company = companies.findByCnpj(normalized);
            if (!ACTIVE_STATUS.equalsIgnoreCase(company.registrationStatus())) {
                throw BrokerRuleException.inactiveCompany();
            }
            PostalAddress address = addresses.findByPostalCode(company.postalCode());
            RegulatoryRegistration regulatory = regulatoryRegistry.findByCnpj(normalized);
            if (!regulatory.activeCtvm()) {
                throw BrokerRuleException.notAuthorized();
            }
            String tradeName = company.tradeName() == null || company.tradeName().isBlank()
                    ? company.legalName()
                    : company.tradeName();
            return new BrokerLookup(normalized, company.legalName(), tradeName,
                    company.registrationStatus(), "CTVM", address.postalCode(), address.street(),
                    address.complement(), address.neighborhood(), address.city(), address.state());
        } catch (ExternalDataFailure failure) {
            throw mapFailure(failure);
        }
    }

    private RuntimeException mapFailure(ExternalDataFailure failure) {
        return switch (failure.reason()) {
            case INVALID_INPUT -> BrokerRuleException.invalidCnpj();
            case NOT_FOUND -> BrokerRuleException.notFound();
            case INCOMPLETE_RESPONSE, RATE_LIMITED, SERVER_ERROR, TIMEOUT,
                    TRANSPORT_ERROR, INVALID_RESPONSE -> new ExternalDependencyException();
        };
    }
}
