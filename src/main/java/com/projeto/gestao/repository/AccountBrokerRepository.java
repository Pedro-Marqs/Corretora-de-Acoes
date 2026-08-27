package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AssociationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface AccountBrokerRepository extends JpaRepository<AccountBroker, UUID> {
    List<AccountBroker> findByAccountIdAndStatus(UUID accountId, AssociationStatus status);
    boolean existsByAccountIdAndBrokerIdAndStatus(UUID accountId, UUID brokerId, AssociationStatus status);
    Optional<AccountBroker> findByAccountIdAndBrokerId(UUID accountId, UUID brokerId);
    Optional<AccountBroker> findByIdAndAccountIdAndStatus(UUID id, UUID accountId, AssociationStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountBroker> findForUpdateByIdAndAccountIdAndStatus(
            UUID id, UUID accountId, AssociationStatus status);
}
