package com.projeto.gestao.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.PostalAddressPort;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;

class ExternalContractsTests {

    @Test
    void portsDoNotExposeInfrastructureDtos() {
        assertThat(Arrays.asList(
                CompanyRegistryPort.class,
                PostalAddressPort.class,
                RegulatoryRegistryPort.class))
                .allSatisfy(type -> Arrays.stream(type.getMethods())
                        .flatMap(method -> Arrays.stream(new Class<?>[] {method.getReturnType()}))
                        .forEach(returnType -> assertThat(returnType.getPackageName())
                                .doesNotContain("infra.client.dto")));
    }

    @Test
    void normalizesMaskedIdentifiersAtBoundary() {
        assertThat(ExternalIdentifierNormalizer.cnpj("02.332.886/0001-04", "test"))
                .isEqualTo("02332886000104");
        assertThat(ExternalIdentifierNormalizer.postalCode("22250-911", "test"))
                .isEqualTo("22250911");
    }

    @Test
    void rejectsStructurallyInvalidIdentifiers() {
        assertThatThrownBy(() -> ExternalIdentifierNormalizer.cnpj("123", "test"))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.reason())
                                .isEqualTo(ExternalDataFailure.Reason.INVALID_INPUT));
    }
}
