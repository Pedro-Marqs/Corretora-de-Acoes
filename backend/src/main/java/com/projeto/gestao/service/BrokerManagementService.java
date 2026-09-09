package com.projeto.gestao.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class BrokerManagementService {

    private final BrokerLookupService lookupService;
    private final BrokerAssociationPersistenceService persistence;

    public BrokerManagementService(BrokerLookupService lookupService,
            BrokerAssociationPersistenceService persistence) {
        this.lookupService = lookupService;
        this.persistence = persistence;
    }

    public BrokerLookup lookup(String cnpj) {
        return lookupService.lookup(cnpj);
    }

    public BrokerAssociationView associate(UUID accountId, String cnpj) {
        BrokerLookup validated = lookupService.lookup(cnpj);
        return persistence.associate(accountId, validated);
    }

    public List<BrokerAssociationView> listActive(UUID accountId) {
        return persistence.listActive(accountId);
    }

    public void remove(UUID accountId, UUID associationId) {
        persistence.remove(accountId, associationId);
    }
}
