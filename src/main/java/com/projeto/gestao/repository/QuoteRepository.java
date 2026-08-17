package com.projeto.gestao.repository;

import java.util.UUID;

import com.projeto.gestao.domain.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
}
