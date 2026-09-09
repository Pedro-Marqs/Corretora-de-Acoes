package com.projeto.gestao.service;

import com.projeto.gestao.api.exception.BrokerRuleException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.CompanyRegistration;
import com.projeto.gestao.domain.model.RegulatoryRegistration;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;
import org.springframework.stereotype.Service;

/** Revalida os dados cadastrais da corretora no momento da confirmação financeira. */
@Service
public class BrokerStatusValidationService {
    private static final String ACTIVE_STATUS = "ATIVA";

    private final CompanyRegistryPort companies;
    private final RegulatoryRegistryPort regulatoryRegistry;

    public BrokerStatusValidationService(CompanyRegistryPort companies,
            RegulatoryRegistryPort regulatoryRegistry) {
        this.companies = companies;
        this.regulatoryRegistry = regulatoryRegistry;
    }

    public void validate(AccountBroker association) {
        if (association == null || association.getBroker() == null) {
            throw BrokerRuleException.notAuthorized();
        }
        String cnpj = association.getBroker().getCnpj();
        try {
            CompanyRegistration company = companies.findByCnpj(cnpj);
            if (company == null) {
                throw new ExternalDependencyException();
            }
            if (!ACTIVE_STATUS.equalsIgnoreCase(company.registrationStatus())) {
                throw BrokerRuleException.inactiveCompany();
            }
            RegulatoryRegistration regulatory = regulatoryRegistry.findByCnpj(cnpj);
            if (regulatory == null) {
                throw new ExternalDependencyException();
            }
            if (!regulatory.activeCtvm()) {
                throw BrokerRuleException.notAuthorized();
            }
        } catch (ExternalDataFailure failure) {
            throw new ExternalDependencyException();
        }
    }

}
