package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class BrokerManagementServiceTests {

    @Test
    void validatesExternallyBeforeStartingPersistencePhase() {
        BrokerLookupService lookupService = mock(BrokerLookupService.class);
        BrokerAssociationPersistenceService persistence = mock(BrokerAssociationPersistenceService.class);
        BrokerLookup lookup = new BrokerLookup("02332886000104", "XP", "XP", "ATIVA", "CTVM",
                "22250911", "Rua", "", "Bairro", "Rio", "RJ");
        UUID accountId = UUID.randomUUID();
        when(lookupService.lookup(lookup.cnpj())).thenReturn(lookup);
        when(persistence.associate(accountId, lookup)).thenReturn(new BrokerAssociationView(
                UUID.randomUUID(), lookup.cnpj(), "XP", "XP", "ATIVA", "CTVM",
                "22250911", "Rua", "", "Bairro", "Rio", "RJ"));

        BrokerAssociationView result = new BrokerManagementService(lookupService, persistence)
                .associate(accountId, lookup.cnpj());

        assertThat(result.cnpj()).isEqualTo(lookup.cnpj());
        InOrder order = inOrder(lookupService, persistence);
        order.verify(lookupService).lookup(lookup.cnpj());
        order.verify(persistence).associate(accountId, lookup);
    }
}
