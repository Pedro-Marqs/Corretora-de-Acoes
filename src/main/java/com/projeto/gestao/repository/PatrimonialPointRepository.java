package com.projeto.gestao.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.domain.model.PatrimonialPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatrimonialPointRepository extends JpaRepository<PatrimonialPoint, UUID> {
    List<PatrimonialPoint> findByAccountIdAndRecordedAtBetweenOrderByRecordedAt(
            UUID accountId, OffsetDateTime start, OffsetDateTime end);
}
