package com.projeto.gestao.repository;

import java.math.BigDecimal;
import java.util.UUID;

import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, UUID>, JpaSpecificationExecutor<Movement> {
    Page<Movement> findByAccountId(UUID accountId, Pageable pageable);

    @Query("select coalesce(sum(movement.realizedResult), 0) from Movement movement "
            + "where movement.account.id = :accountId and movement.movementType = :type")
    BigDecimal sumRealizedResultByAccountIdAndType(
            @Param("accountId") UUID accountId, @Param("type") MovementType type);
}
