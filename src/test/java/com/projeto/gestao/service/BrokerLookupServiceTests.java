package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.projeto.gestao.api.exception.BrokerRuleException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.CompanyRegistration;
import com.projeto.gestao.domain.model.PostalAddress;
import com.projeto.gestao.domain.model.RegulatoryParticipant;
import com.projeto.gestao.domain.model.RegulatoryRegistration;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.PostalAddressPort;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;

class BrokerLookupServiceTests {

    private static final String CNPJ = "02332886000104";

    private CompanyRegistryPort companies;
    private PostalAddressPort addresses;
    private RegulatoryRegistryPort registry;
    private BrokerLookupService service;

    @BeforeEach
    void setUp() {
        companies = mock(CompanyRegistryPort.class);
        addresses = mock(PostalAddressPort.class);
        registry = mock(RegulatoryRegistryPort.class);
        service = new BrokerLookupService(companies, addresses, registry);
    }

    @Test
    void consolidatesEligibleCtvmWithoutPersistence() {
        validCompany("ATIVA");
        when(addresses.findByPostalCode("22250911")).thenReturn(address());
        when(registry.findByCnpj(CNPJ)).thenReturn(regulatory(true));

        BrokerLookup result = service.lookup("02.332.886/0001-04");

        assertThat(result.cnpj()).isEqualTo(CNPJ);
        assertThat(result.cvmCategory()).isEqualTo("CTVM");
        assertThat(result.city()).isEqualTo("Rio de Janeiro");
    }

    @Test
    void rejectsInactiveCompanyBeforeAddressAndRegulatoryCalls() {
        validCompany("BAIXADA");
        assertThatThrownBy(() -> service.lookup(CNPJ))
                .isInstanceOfSatisfying(BrokerRuleException.class,
                        failure -> assertThat(failure.publicMessage()).contains("não está cadastralmente ativo"));
        verifyNoInteractions(addresses, registry);
    }

    @Test
    void rejectsCompanyThatIsNotCtvm() {
        validCompany("ATIVA");
        when(addresses.findByPostalCode("22250911")).thenReturn(address());
        when(registry.findByCnpj(CNPJ)).thenReturn(regulatory(false));
        assertThatThrownBy(() -> service.lookup(CNPJ)).isInstanceOf(BrokerRuleException.class);
    }

    @Test
    void keepsExternalUnavailabilityDistinctFromRegulatoryRejection() {
        when(companies.findByCnpj(CNPJ)).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "BrasilAPI", "timeout"));
        assertThatThrownBy(() -> service.lookup(CNPJ)).isInstanceOf(ExternalDependencyException.class);
        verifyNoInteractions(addresses, registry);
    }

    private void validCompany(String status) {
        when(companies.findByCnpj(CNPJ)).thenReturn(new CompanyRegistration(
                CNPJ, "XP INVESTIMENTOS S/A", "XP", status, "22250911"));
    }

    private static PostalAddress address() {
        return new PostalAddress("22250911", "Praia Botafogo", "", "Botafogo", "Rio de Janeiro", "RJ");
    }

    private static RegulatoryRegistration regulatory(boolean activeCtvm) {
        return new RegulatoryRegistration(CNPJ, activeCtvm, activeCtvm,
                activeCtvm ? List.of(new RegulatoryParticipant(
                        "CORRETORAS", "XP", "XP", "EM FUNCIONAMENTO NORMAL", "3247")) : List.of());
    }
}
