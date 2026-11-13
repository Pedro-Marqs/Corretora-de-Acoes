package com.projeto.gestao.repository;

import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminStatus;
import com.projeto.gestao.domain.model.AdminUser;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select admin from AdminUser admin where lower(admin.email) = lower(:email)")
    Optional<AdminUser> findForUpdateByEmailIgnoreCase(@Param("email") String email);

    Optional<AdminUser> findFirstByOrderByCreatedAtAsc();
    Optional<AdminUser> findByIdAndStatus(UUID id, AdminStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select admin from AdminUser admin where admin.id = :id")
    Optional<AdminUser> findForUpdateById(@Param("id") UUID id);
    long countByStatus(AdminStatus status);
}
