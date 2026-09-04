package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByAccountIdAndQuantityGreaterThan(UUID accountId, long quantity);
    Optional<Position> findByAccountIdAndAccountBrokerIdAndAssetId(UUID accountId, UUID accountBrokerId, UUID assetId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Position> findForUpdateByAccountIdAndAccountBrokerIdAndAssetId(
            UUID accountId, UUID accountBrokerId, UUID assetId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select position from Position position "
            + "where position.account.id = :accountId and position.asset.id = :assetId "
            + "and position.accountBroker.id in :brokerIds order by position.accountBroker.id")
    List<Position> findAllForUpdateOrdered(@Param("accountId") UUID accountId,
            @Param("assetId") UUID assetId, @Param("brokerIds") List<UUID> brokerIds);
    boolean existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
            UUID accountId, UUID accountBrokerId, long quantity);
}
