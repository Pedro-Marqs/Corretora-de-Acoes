package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByAccountIdAndQuantityGreaterThan(UUID accountId, long quantity);
    Optional<Position> findByAccountIdAndAccountBrokerIdAndAssetId(UUID accountId, UUID accountBrokerId, UUID assetId);
    boolean existsByAccountIdAndAccountBrokerIdAndQuantityGreaterThan(
            UUID accountId, UUID accountBrokerId, long quantity);
}
