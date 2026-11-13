package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.api.exception.ConflictException;
import com.projeto.gestao.api.exception.ResourceNotFoundException;
import com.projeto.gestao.api.exception.ValidationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.AdminAuditAction;
import com.projeto.gestao.domain.model.AdminAuditOutcome;
import com.projeto.gestao.domain.model.AuditActorRole;
import com.projeto.gestao.domain.dto.AdminInvestorPage;
import com.projeto.gestao.domain.dto.AdminInvestorProjection;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.SessionRevocationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminInvestorServiceTests {
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-10T12:00:00Z");

    @Mock private AccountRepository accountRepository;
    @Mock private SessionRevocationRepository sessionRevocationRepository;
    @Mock private ObjectProvider<RefreshTokenService> refreshTokenProvider;
    @Mock private AdminAuditService auditService;
    @Mock private RefreshTokenService refreshTokenService;

    private AdminInvestorService service;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new AdminInvestorService(accountRepository, sessionRevocationRepository,
                refreshTokenProvider, auditService,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        account = Account.create(ACCOUNT_ID, "Investidor", "52998224725", "investor@example.com",
                "hash", new BigDecimal("10000.00"), NOW.minusDays(10));
    }

    @Test
    void listCoversOptionalFiltersAndNormalizesSearch() {
        PageRequest pageable = PageRequest.of(0, 20);
        AdminInvestorProjection projection = org.mockito.Mockito.mock(AdminInvestorProjection.class);
        when(projection.getId()).thenReturn(ACCOUNT_ID);
        when(projection.getName()).thenReturn("Investidor");
        when(projection.getEmail()).thenReturn("investor@example.com");
        when(projection.getCpf()).thenReturn("52998224725");
        when(projection.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(projection.getCreatedAt()).thenReturn(account.getCreatedAt());
        when(projection.getInactivatedAt()).thenReturn(null);
        when(accountRepository.findForAdministrativeView(any(), any(), any(), any(), any(), any(),
                eq(pageable))).thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        AdminInvestorPage withoutFilters = service.list(null, null, null, null, null, null,
                pageable, ADMIN_ID, CORRELATION_ID);
        assertThat(withoutFilters.content()).hasSize(1);

        OffsetDateTime from = NOW.minusDays(20);
        OffsetDateTime to = NOW.plusDays(1);
        service.list(AccountStatus.ACTIVE, "  investor@example.com  ", from, to,
                from, to, pageable, ADMIN_ID, CORRELATION_ID);
        service.list(AccountStatus.ACTIVE, "   ", from, null, from, null,
                pageable, ADMIN_ID, CORRELATION_ID);

        verify(accountRepository).findForAdministrativeView(
                eq(AccountStatus.ACTIVE), eq("investor@example.com"), eq(from), eq(to),
                eq(from), eq(to), eq(pageable));
        verify(auditService, org.mockito.Mockito.times(3)).record(
                eq(ADMIN_ID), eq(AuditActorRole.ADMIN), eq(AdminAuditAction.LIST_INVESTORS),
                eq(null), eq(null), eq(AdminAuditOutcome.SUCCESS), eq(CORRELATION_ID));
    }

    @Test
    void rejectsBothInvalidDateRangesBeforeQuerying() {
        OffsetDateTime later = NOW.plusDays(1);
        OffsetDateTime earlier = NOW.minusDays(1);

        assertThatThrownBy(() -> service.list(null, null, later, earlier, null, null,
                PageRequest.of(0, 20), ADMIN_ID, CORRELATION_ID))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.list(null, null, null, null, later, earlier,
                PageRequest.of(0, 20), ADMIN_ID, CORRELATION_ID))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void activatesAndDeactivatesWithOptionalRefreshProvider() {
        when(accountRepository.findForUpdateById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(refreshTokenProvider.getIfAvailable()).thenReturn(null);

        service.changeStatus(ACCOUNT_ID, AccountStatus.INACTIVE, ADMIN_ID, CORRELATION_ID);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(sessionRevocationRepository).revokeAll(ACCOUNT_ID);

        service.changeStatus(ACCOUNT_ID, AccountStatus.ACTIVE, ADMIN_ID, CORRELATION_ID);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountRepository, org.mockito.Mockito.times(2)).saveAndFlush(account);
    }

    @Test
    void revokesRefreshTokensWhenProviderIsAvailable() {
        when(accountRepository.findForUpdateById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(refreshTokenProvider.getIfAvailable()).thenReturn(refreshTokenService);

        service.changeStatus(ACCOUNT_ID, AccountStatus.INACTIVE, ADMIN_ID, CORRELATION_ID);

        verify(refreshTokenService).revokeAllForAccount(ACCOUNT_ID);
    }

    @Test
    void auditsMissingAndInvalidTransitionsWithoutMutatingAccount() {
        UUID missing = UUID.randomUUID();
        when(accountRepository.findForUpdateById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeStatus(
                missing, AccountStatus.INACTIVE, ADMIN_ID, CORRELATION_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        when(accountRepository.findForUpdateById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        assertThatThrownBy(() -> service.changeStatus(
                ACCOUNT_ID, AccountStatus.ACTIVE, ADMIN_ID, CORRELATION_ID))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.changeStatus(
                ACCOUNT_ID, null, ADMIN_ID, CORRELATION_ID))
                .isInstanceOf(ConflictException.class);

        verify(auditService, org.mockito.Mockito.times(3)).recordInNewTransaction(
                eq(ADMIN_ID), eq(AuditActorRole.ADMIN), eq(AdminAuditAction.ACCESS_DENIED),
                any(), eq(null), eq(AdminAuditOutcome.DENIED), eq(CORRELATION_ID));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void auditsUnexpectedFailureAndRethrowsIt() {
        RuntimeException failure = new IllegalStateException("database failure");
        when(accountRepository.findForUpdateById(ACCOUNT_ID)).thenThrow(failure);

        assertThatThrownBy(() -> service.changeStatus(
                ACCOUNT_ID, AccountStatus.INACTIVE, ADMIN_ID, CORRELATION_ID))
                .isSameAs(failure);
        verify(auditService).recordInNewTransaction(
                eq(ADMIN_ID), eq(AuditActorRole.ADMIN), eq(AdminAuditAction.ACCESS_DENIED),
                eq(null), eq(null), eq(AdminAuditOutcome.FAILURE), eq(CORRELATION_ID));
    }
}
