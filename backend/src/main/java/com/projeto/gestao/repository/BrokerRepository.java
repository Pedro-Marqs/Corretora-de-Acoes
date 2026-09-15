package com.projeto.gestao.repository;

import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Broker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrokerRepository extends JpaRepository<Broker, UUID> {
    Optional<Broker> findByCnpj(String cnpj);
}
