package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.AssetStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PurchaseServiceTests {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountBrokerRepository associations = mock(AccountBrokerRepository.class);
    private final AssetRepository assets = mock(AssetRepository.class);
    private final AssetCatalogService market = mock(AssetCatalogService.class);
    private final PurchaseTransactionService transactions = mock(PurchaseTransactionService.class);
    private PurchaseService service;
    private Account account;
    private Asset asset;
    private AccountBroker association;

    @BeforeEach
    void setUp() {
        service = new PurchaseService(accounts, associations, assets, market, transactions);
        account = Account.create(UUID.randomUUID(), "Investor", "52998224725", "buyer@example.com",
                "hash", new BigDecimal("1000.00"), OffsetDateTime.now());
        asset = new Asset("AAPL", "Apple", Market.US, Currency.USD);
        Broker broker = Broker.create(UUID.randomUUID(), "02332886000104", "XP SA", "XP", "ATIVA",
                "CTVM", "01001000", "Rua A", "1", null, "Centro", "São Paulo", "SP",
                OffsetDateTime.now());
        association = AccountBroker.create(UUID.randomUUID(), account, broker, OffsetDateTime.now());
        when(accounts.findByIdAndStatus(account.getId(), AccountStatus.ACTIVE)).thenReturn(Optional.of(account));
        when(assets.findByIdAndStatus(asset.getId(), AssetStatus.ACTIVE)).thenReturn(Optional.of(asset));
        when(associations.findByIdAndAccountIdAndStatus(
                association.getId(), account.getId(), AssociationStatus.ACTIVE))
                .thenReturn(Optional.of(association));
    }

    @Test
    void resolvesAndConvertsMarketBeforeCallingTransactionalPhase() {
        when(market.find("AAPL", Market.US)).thenReturn(new AssetPriceView(
                "AAPL", "Apple", Market.US, Currency.USD, new BigDecimal("10.005"),
                new BigDecimal("50.15"), "TwelveData", Instant.parse("2026-09-03T13:00:00Z"),
                false, new BigDecimal("5.005"), "AwesomeAPI",
                Instant.parse("2026-09-03T12:00:00Z"), false));
        ArgumentCaptor<PurchaseQuote> quote = ArgumentCaptor.forClass(PurchaseQuote.class);

        service.purchase(account.getId(), asset.getId(), association.getId(), 2);

        verify(market).find("AAPL", Market.US);
        verify(transactions).purchase(eq(account.getId()), eq(association.getId()), eq(2L), quote.capture());
        org.assertj.core.api.Assertions.assertThat(quote.getValue().unitPriceBrl())
                .isEqualByComparingTo("50.15");
    }

    @Test
    void rejectsInvalidIntentBeforeMarketOrTransaction() {
        assertThatThrownBy(() -> service.purchase(account.getId(), asset.getId(), association.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        verify(market, never()).find(any(), any());
        verify(transactions, never()).purchase(any(), any(), eq(0L), any());
    }

    @Test
    void rejectsMissingSessionBeforeAccessingAccountData() {
        assertThatThrownBy(() -> service.purchase(null, asset.getId(), association.getId(), 1))
                .isInstanceOf(AuthenticationException.class);
        verify(accounts, never()).findByIdAndStatus(any(), any());
        verify(market, never()).find(any(), any());
    }
}
