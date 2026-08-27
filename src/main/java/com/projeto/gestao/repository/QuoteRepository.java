package com.projeto.gestao.repository;

import java.util.UUID;

import com.projeto.gestao.domain.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Quote> findByAssetId(UUID assetId);
}
