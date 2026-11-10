package com.projeto.gestao.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.AdminRefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AdminRefreshToken token where token.tokenHash = :tokenHash")
    Optional<AdminRefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AdminRefreshToken token set token.revokedAt = :revokedAt "
            + "where token.familyId = :familyId and token.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") OffsetDateTime revokedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AdminRefreshToken token set token.revokedAt = :revokedAt "
            + "where token.adminUserId = :adminUserId and token.revokedAt is null")
    int revokeAllForAdmin(@Param("adminUserId") UUID adminUserId,
            @Param("revokedAt") OffsetDateTime revokedAt);
}
