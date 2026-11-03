package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.AssetStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.PositionBalance;
import com.projeto.gestao.domain.model.PositionQuantity;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionSnapshotValidationTests {
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BROKER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final Instant QUOTED_AT = Instant.parse("2026-08-27T12:00:00Z");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-27T09:00:00-03:00");

    @Mock AccountRepository accounts;
    @Mock AccountBrokerRepository accountBrokers;
    @Mock AssetRepository assets;
    @Mock PositionRepository positions;
    @Mock QuoteRepository quotes;
    @Mock ExchangeRateRepository rates;
    @Mock FinancialHistoryService history;

    private Account account;
    private AccountBroker association;
    private Asset brlAsset;
    private Quote brlQuote;
    private PurchaseTransactionService purchases;
    private SaleTransactionService sales;

    @BeforeEach
    void setUp() {
        account = Account.create(ACCOUNT_ID, "Investor", "12345678901", "investor@example.com",
                "hash", amount("1000"), OCCURRED_AT);
        Broker broker = Broker.create(BROKER_ID, "12345678000199", "Broker SA", "Broker",
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro",
                "Sao Paulo", "SP", OCCURRED_AT);
        association = AccountBroker.create(BROKER_ID, account, broker, OCCURRED_AT);
        brlAsset = new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL);
        brlQuote = new Quote(brlAsset, amount("10"), Currency.BRL,
                QUOTED_AT.atOffset(ZoneOffset.UTC), QUOTED_AT.atOffset(ZoneOffset.UTC), "cache");

        lenient().when(accounts.findForUpdateByIdAndStatus(ACCOUNT_ID, AccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
        lenient().when(accountBrokers.findForUpdateByIdAndAccountIdAndStatus(
                BROKER_ID, ACCOUNT_ID, AssociationStatus.ACTIVE)).thenReturn(Optional.of(association));
        lenient().when(assets.findByIdAndStatus(brlAsset.getId(), AssetStatus.ACTIVE))
                .thenReturn(Optional.of(brlAsset));
        lenient().when(quotes.findById(brlAsset.getId())).thenReturn(Optional.of(brlQuote));
        lenient().when(positions.findForUpdateByAccountIdAndAccountBrokerIdAndAssetId(
                ACCOUNT_ID, BROKER_ID, brlAsset.getId())).thenReturn(Optional.empty());
        lenient().when(positions.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(rates.findById(PatrimonyCalculator.USD_BRL)).thenReturn(Optional.empty());

        Clock clock = Clock.fixed(QUOTED_AT, ZoneOffset.UTC);
        purchases = new PurchaseTransactionService(accounts, accountBrokers, assets, positions,
                quotes, rates, history, clock);
        sales = new SaleTransactionService(accounts, accountBrokers, assets, positions,
                quotes, rates, history, clock);
    }

    @Test
    void purchaseRejectsInvalidSnapshotFieldsBeforeChangingBalance() {
        PurchaseQuote valid = purchaseQuote(brlAsset, amount("10"), amount("10"));
        PurchaseQuote[] invalid = {
            new PurchaseQuote(UUID.randomUUID(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), "WRONG", valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), Market.US, Currency.USD,
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), Currency.USD,
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    null, valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    amount("0"), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", null, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), null, "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), amount("0"), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    amount("11"), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new PurchaseQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT.plusSeconds(1), false,
                    null, null, null, null)
        };

        for (PurchaseQuote candidate : invalid) {
            assertThatThrownBy(() -> purchases.purchase(ACCOUNT_ID, BROKER_ID, 1, candidate, OCCURRED_AT))
                    .isInstanceOf(RuntimeException.class);
        }
        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void saleRejectsInvalidSnapshotAndConfirmedPriceBeforeChangingPosition() {
        Position position = Position.create(UUID.randomUUID(), account, association, brlAsset,
                new PositionBalance(PositionQuantity.positive(2), amountObject("20"), amountObject("10")));
        SaleQuote valid = saleQuote(brlAsset, amount("10"), amount("10"));
        SaleQuote[] invalid = {
            new SaleQuote(UUID.randomUUID(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new SaleQuote(brlAsset.getId(), "WRONG", valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new SaleQuote(brlAsset.getId(), valid.ticker(), Market.US, Currency.USD,
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null),
            new SaleQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null, null),
            new SaleQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), valid.unitPriceBrl(), "cache", QUOTED_AT, false,
                    null, null, null, null, amount("0")),
            new SaleQuote(brlAsset.getId(), valid.ticker(), valid.market(), valid.currency(),
                    valid.originalPrice(), amount("0"), "cache", QUOTED_AT, false,
                    null, null, null, null)
        };

        for (SaleQuote candidate : invalid) {
            assertThatThrownBy(() -> sales.sell(ACCOUNT_ID, BROKER_ID, 1, candidate, OCCURRED_AT))
                    .isInstanceOf(RuntimeException.class);
        }
        assertThat(position.getQuantity()).isEqualTo(2);
    }

    @Test
    void validatesUsRateAndCompletesBrazilianAndUsTransactions() {
        PurchaseQuote brlPurchase = purchaseQuote(brlAsset, amount("10"), amount("10"));
        assertThat(purchases.purchase(ACCOUNT_ID, BROKER_ID, 1, brlPurchase, null).purchaseAmountBrl())
                .isEqualByComparingTo("10.00");

        Asset usAsset = new Asset("AAPL", "Apple", Market.US, Currency.USD);
        Quote usQuote = new Quote(usAsset, amount("10"), Currency.USD,
                QUOTED_AT.atOffset(ZoneOffset.UTC), QUOTED_AT.atOffset(ZoneOffset.UTC), "cache");
        ExchangeRate rate = new ExchangeRate("USD/BRL", amount("5"),
                QUOTED_AT.atOffset(ZoneOffset.UTC), QUOTED_AT.atOffset(ZoneOffset.UTC), "cache");
        when(assets.findByIdAndStatus(usAsset.getId(), AssetStatus.ACTIVE)).thenReturn(Optional.of(usAsset));
        when(quotes.findById(usAsset.getId())).thenReturn(Optional.of(usQuote));
        when(rates.findById(PatrimonyCalculator.USD_BRL)).thenReturn(Optional.of(rate));
        PurchaseQuote usPurchase = purchaseQuote(usAsset, amount("10"), amount("50"));

        assertThat(purchases.purchase(ACCOUNT_ID, BROKER_ID, 1, usPurchase, OCCURRED_AT)
                .purchaseAmountBrl()).isEqualByComparingTo("50.00");
    }

    private PurchaseQuote purchaseQuote(Asset asset, BigDecimal original, BigDecimal unitBrl) {
        return new PurchaseQuote(asset.getId(), asset.getTicker(), asset.getMarket(), asset.getCurrency(),
                original, unitBrl, "cache", QUOTED_AT, false,
                asset.getCurrency() == Currency.USD ? amount("5") : null,
                asset.getCurrency() == Currency.USD ? "cache" : null,
                asset.getCurrency() == Currency.USD ? QUOTED_AT : null, false);
    }

    private SaleQuote saleQuote(Asset asset, BigDecimal original, BigDecimal unitBrl) {
        return new SaleQuote(asset.getId(), asset.getTicker(), asset.getMarket(), asset.getCurrency(),
                original, unitBrl, "cache", QUOTED_AT, false, null, null, null, false);
    }

    private static com.projeto.gestao.domain.model.FinancialAmount amountObject(String value) {
        return new com.projeto.gestao.domain.model.FinancialAmount(amount(value));
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
