package com.projeto.gestao.repository;

import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AssociationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBrokerRepository extends JpaRepository<AccountBroker, UUID> {
    List<AccountBroker> findByAccountIdAndStatus(UUID accountId, AssociationStatus status);
    boolean existsByAccountIdAndBrokerIdAndStatus(UUID accountId, UUID brokerId, AssociationStatus status);
}
