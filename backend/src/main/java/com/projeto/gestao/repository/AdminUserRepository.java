package com.projeto.gestao.repository;

import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByEmailIgnoreCase(String email);
    Optional<AdminUser> findFirstByOrderByCreatedAtAsc();
    Optional<AdminUser> findByIdAndStatus(UUID id, AdminStatus status);
    long countByStatus(AdminStatus status);
}
