package com.projeto.gestao.repository;

import java.util.UUID;

import com.projeto.gestao.domain.model.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovementRepository extends JpaRepository<Movement, UUID>, JpaSpecificationExecutor<Movement> {
    Page<Movement> findByAccountId(UUID accountId, Pageable pageable);
}
