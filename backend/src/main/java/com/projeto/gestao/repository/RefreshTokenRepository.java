package com.projeto.gestao.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.RefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
            + "where token.familyId = :familyId and token.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") OffsetDateTime revokedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
            + "where token.accountId = :accountId and token.revokedAt is null")
    int revokeAllForAccount(@Param("accountId") UUID accountId, @Param("revokedAt") OffsetDateTime revokedAt);
}
