package com.projeto.gestao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.dto.AdminInvestorProjection;
import com.projeto.gestao.domain.dto.AdminDashboardAccountCreatedProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailIgnoreCaseAndStatus(String email, AccountStatus status);
    Optional<Account> findByIdAndStatus(UUID id, AccountStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findForUpdateByIdAndStatus(UUID id, AccountStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.id = :id")
    Optional<Account> findForUpdateById(@Param("id") UUID id);
    @Query(value = """
            select account.id as id, account.name as name, account.email as email,
                   account.cpf as cpf, account.status as status,
                   account.createdAt as createdAt, account.inactivatedAt as inactivatedAt
            from Account account
            where (:status is null or account.status = :status)
              and (:search = ''
                   or lower(account.name) like lower(concat('%', :search, '%'))
                   or lower(account.email) like lower(concat('%', :search, '%'))
                   or account.cpf like concat('%', :search, '%'))
              and account.createdAt >= coalesce(:createdFrom, account.createdAt)
              and account.createdAt <= coalesce(:createdTo, account.createdAt)
              and (coalesce(:inactivatedFrom, account.inactivatedAt) is null
                   or account.inactivatedAt >= coalesce(:inactivatedFrom, account.inactivatedAt))
              and (coalesce(:inactivatedTo, account.inactivatedAt) is null
                   or account.inactivatedAt <= coalesce(:inactivatedTo, account.inactivatedAt))
            """, countQuery = """
            select count(account.id) from Account account
            where (:status is null or account.status = :status)
              and (:search = ''
                   or lower(account.name) like lower(concat('%', :search, '%'))
                   or lower(account.email) like lower(concat('%', :search, '%'))
                   or account.cpf like concat('%', :search, '%'))
              and account.createdAt >= coalesce(:createdFrom, account.createdAt)
              and account.createdAt <= coalesce(:createdTo, account.createdAt)
              and (coalesce(:inactivatedFrom, account.inactivatedAt) is null
                   or account.inactivatedAt >= coalesce(:inactivatedFrom, account.inactivatedAt))
              and (coalesce(:inactivatedTo, account.inactivatedAt) is null
                   or account.inactivatedAt <= coalesce(:inactivatedTo, account.inactivatedAt))
            """)
    Page<AdminInvestorProjection> findForAdministrativeView(
            @Param("status") AccountStatus status,
            @Param("search") String search,
            @Param("createdFrom") java.time.OffsetDateTime createdFrom,
            @Param("createdTo") java.time.OffsetDateTime createdTo,
            @Param("inactivatedFrom") java.time.OffsetDateTime inactivatedFrom,
            @Param("inactivatedTo") java.time.OffsetDateTime inactivatedTo,
            Pageable pageable);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatus(
            java.time.OffsetDateTime createdFrom, java.time.OffsetDateTime createdTo,
            AccountStatus status);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatusIn(
            java.time.OffsetDateTime createdFrom, java.time.OffsetDateTime createdTo,
            java.util.Collection<AccountStatus> statuses);
    long countByStatus(AccountStatus status);
    long countByStatusIn(java.util.Collection<AccountStatus> statuses);
    @Query("select min(account.createdAt) from Account account")
    java.time.OffsetDateTime findFirstCreatedAt();
    @Query("select account.createdAt as createdAt from Account account "
            + "where account.createdAt >= :createdFrom and account.createdAt < :createdTo")
    java.util.List<AdminDashboardAccountCreatedProjection> findCreatedAtBetween(
            @Param("createdFrom") java.time.OffsetDateTime createdFrom,
            @Param("createdTo") java.time.OffsetDateTime createdTo);
    boolean existsByEmailIgnoreCaseAndStatus(String email, AccountStatus status);
    boolean existsByEmailIgnoreCaseAndStatusAndIdNot(String email, AccountStatus status, UUID id);
    boolean existsByCpfAndStatus(String cpf, AccountStatus status);
    List<Account> findAllByCpfAndStatus(String cpf, AccountStatus status);
}
