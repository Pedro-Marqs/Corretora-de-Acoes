package com.projeto.gestao.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import com.projeto.gestao.domain.model.CompanyRegistration;
import com.projeto.gestao.domain.model.RegulatoryRegistration;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;

public final class ExternalRegistryTestStubs {
    private ExternalRegistryTestStubs() {
    }

    public static void active(CompanyRegistryPort companies, RegulatoryRegistryPort regulatoryRegistry) {
        when(companies.findByCnpj(anyString())).thenAnswer(invocation -> new CompanyRegistration(
                invocation.getArgument(0), "Corretora", "Corretora", "ATIVA", "01001000"));
        when(regulatoryRegistry.findByCnpj(anyString())).thenAnswer(invocation -> new RegulatoryRegistration(
                invocation.getArgument(0), true, true, List.of()));
    }
}
