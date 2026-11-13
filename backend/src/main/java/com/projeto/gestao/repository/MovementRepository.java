package com.projeto.gestao.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.dto.AdminDashboardMovementDayProjection;
import com.projeto.gestao.domain.dto.AdminDashboardMovementSummaryProjection;
import com.projeto.gestao.domain.dto.AdminDashboardRankingProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, UUID>, JpaSpecificationExecutor<Movement> {
    List<Movement> findByAccountIdAndOccurredAtLessThanEqualOrderByOccurredAtAscIdAsc(
            UUID accountId, OffsetDateTime occurredAt);

    Page<Movement> findByAccountId(UUID accountId, Pageable pageable);

    @Query("select coalesce(sum(movement.realizedResult), 0) from Movement movement "
            + "where movement.account.id = :accountId and movement.movementType = :type")
    BigDecimal sumRealizedResultByAccountIdAndType(
            @Param("accountId") UUID accountId, @Param("type") MovementType type);

    @Query("select coalesce(sum(movement.realizedResult), 0) from Movement movement "
            + "where movement.account.id = :accountId and movement.movementType = :type "
            + "and movement.brokerName = :brokerName")
    BigDecimal sumRealizedResultByAccountIdAndTypeAndBrokerName(
            @Param("accountId") UUID accountId, @Param("type") MovementType type,
            @Param("brokerName") String brokerName);

    @Query("select movement.movementType as movementType, movement.currency as currency, "
            + "count(movement.id) as operationCount, coalesce(sum(movement.quantity), 0) as shares, "
            + "coalesce(sum(case when movement.currency = :brl then movement.totalAmount "
            + "else movement.quotePrice * movement.quantity end), 0) as nativeVolume "
            + "from Movement movement "
            + "where movement.occurredAt >= :from and movement.occurredAt < :to "
            + "and (movement.movementType = :purchase or movement.movementType = :sale) "
            + "group by movement.movementType, movement.currency")
    List<AdminDashboardMovementSummaryProjection> summarizeAdministrativeDashboard(
            @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
            @Param("purchase") MovementType purchase, @Param("sale") MovementType sale,
            @Param("brl") Currency brl);

    @Query("select movement.occurredAt as occurredAt, movement.movementType as movementType "
            + "from Movement movement "
            + "where movement.occurredAt >= :from and movement.occurredAt < :to "
            + "and (movement.movementType = :purchase or movement.movementType = :sale)")
    List<AdminDashboardMovementDayProjection> findAdministrativeDashboardDays(
            @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
            @Param("purchase") MovementType purchase, @Param("sale") MovementType sale);

    @Query("select movement.ticker as ticker, movement.market as market, "
            + "sum(movement.quantity) as tradedShares from Movement movement "
            + "where movement.occurredAt >= :from and movement.occurredAt < :to "
            + "and (movement.movementType = :purchase or movement.movementType = :sale) "
            + "group by movement.ticker, movement.market "
            + "order by sum(movement.quantity) desc, movement.ticker asc, movement.market asc")
    List<AdminDashboardRankingProjection> findAdministrativeDashboardRanking(
            @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
            @Param("purchase") MovementType purchase, @Param("sale") MovementType sale,
            org.springframework.data.domain.Pageable pageable);

    @Query("select min(movement.occurredAt) from Movement movement")
    OffsetDateTime findFirstOccurredAt();
}
