package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);
    Optional<Account> findByIdAndStatus(UUID id, AccountStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findForUpdateByIdAndStatus(UUID id, AccountStatus status);
    boolean existsByEmailIgnoreCaseAndStatus(String email, AccountStatus status);
    boolean existsByEmailIgnoreCaseAndStatusAndIdNot(String email, AccountStatus status, UUID id);
    boolean existsByCpfAndStatus(String cpf, AccountStatus status);
    List<Account> findAllByCpfAndStatus(String cpf, AccountStatus status);
}
