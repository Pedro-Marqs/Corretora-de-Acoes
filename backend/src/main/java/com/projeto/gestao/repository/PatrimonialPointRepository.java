package com.projeto.gestao.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.PatrimonialPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatrimonialPointRepository extends JpaRepository<PatrimonialPoint, UUID> {
    List<PatrimonialPoint> findByAccountIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(
            UUID accountId, OffsetDateTime start, OffsetDateTime end);

    Optional<PatrimonialPoint> findTopByAccountIdAndRecordedAtLessThanEqualOrderByRecordedAtDescIdDesc(
            UUID accountId, OffsetDateTime recordedAt);

    List<PatrimonialPoint> findByAccountIdAndRecordedAtLessThanEqualOrderByRecordedAtAscIdAsc(
            UUID accountId, OffsetDateTime end);
}
