package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AssociationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AccountBrokerRepository extends JpaRepository<AccountBroker, UUID> {
    List<AccountBroker> findByAccountIdAndStatus(UUID accountId, AssociationStatus status);
    boolean existsByAccountIdAndBrokerIdAndStatus(UUID accountId, UUID brokerId, AssociationStatus status);
    Optional<AccountBroker> findByAccountIdAndBrokerId(UUID accountId, UUID brokerId);
    Optional<AccountBroker> findByIdAndAccountIdAndStatus(UUID id, UUID accountId, AssociationStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountBroker> findForUpdateByIdAndAccountIdAndStatus(
            UUID id, UUID accountId, AssociationStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select association from AccountBroker association "
            + "where association.account.id = :accountId and association.id in :ids "
            + "order by association.id")
    List<AccountBroker> findAllForUpdateOrdered(@Param("accountId") UUID accountId,
            @Param("ids") List<UUID> ids);
}
