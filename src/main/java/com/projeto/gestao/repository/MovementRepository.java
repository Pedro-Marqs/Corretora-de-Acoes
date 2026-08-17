package com.projeto.gestao.repository;

import java.util.UUID;

import com.projeto.gestao.domain.model.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<Movement, UUID> {
    Page<Movement> findByAccountId(UUID accountId, Pageable pageable);
}
