package com.projeto.gestao.repository;

import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);
    Optional<Account> findByIdAndStatus(UUID id, AccountStatus status);
    boolean existsByEmailIgnoreCaseAndStatus(String email, AccountStatus status);
    boolean existsByEmailIgnoreCaseAndStatusAndIdNot(String email, AccountStatus status, UUID id);
    boolean existsByCpfAndStatus(String cpf, AccountStatus status);
}
