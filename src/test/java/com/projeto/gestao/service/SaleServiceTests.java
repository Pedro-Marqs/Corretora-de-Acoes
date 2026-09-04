package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
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

class SaleServiceTests {
    private static final UUID ASSET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountBrokerRepository associations = mock(AccountBrokerRepository.class);
    private final AssetRepository assets = mock(AssetRepository.class);
    private final AssetCatalogService market = mock(AssetCatalogService.class);
    private final SaleTransactionService transactions = mock(SaleTransactionService.class);
    private SaleService service;
    private Account account;
    private Asset asset;
    private AccountBroker association;

    @BeforeEach
    void setUp() {
        service = new SaleService(accounts, associations, assets, market, transactions);
        account = Account.create(UUID.randomUUID(), "Investor", "52998224725", "seller@example.com",
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
                ASSET_ID, "AAPL", "Apple", Market.US, Currency.USD, new BigDecimal("10.005"),
                new BigDecimal("50.15"), "TwelveData", Instant.parse("2026-09-03T13:00:00Z"),
                true, new BigDecimal("5.005"), "AwesomeAPI",
                Instant.parse("2026-09-02T12:00:00Z"), true));
        ArgumentCaptor<SaleQuote> quote = ArgumentCaptor.forClass(SaleQuote.class);

        service.sell(account.getId(), asset.getId(), association.getId(), 2);

        verify(transactions).sell(eq(account.getId()), eq(association.getId()), eq(2L), quote.capture());
        assertThat(quote.getValue().unitPriceBrl()).isEqualByComparingTo("50.15");
        assertThat(quote.getValue().quoteQuotedAt()).isEqualTo(Instant.parse("2026-09-03T13:00:00Z"));
        assertThat(quote.getValue().exchangeRateQuotedAt()).isEqualTo(Instant.parse("2026-09-02T12:00:00Z"));
        assertThat(quote.getValue().quoteStale()).isTrue();
    }

    @Test
    void rejectsInvalidIntentAndMissingSessionBeforeMarket() {
        assertThatThrownBy(() -> service.sell(account.getId(), asset.getId(), association.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.sell(null, asset.getId(), association.getId(), 1))
                .isInstanceOf(AuthenticationException.class);
        verify(market, never()).find(any(), any());
    }
}
