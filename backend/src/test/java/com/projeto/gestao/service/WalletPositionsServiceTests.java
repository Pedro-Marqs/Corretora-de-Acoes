package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalletPositionsServiceTests {
    private static final OffsetDateTime QUOTED_AT =
            OffsetDateTime.parse("2026-09-08T12:00:00-03:00");

    private AccountRepository accounts;
    private PositionRepository positions;
    private QuoteRepository quotes;
    private ExchangeRateRepository exchangeRates;
    private MarketDataFreshness freshness;
    private WalletPositionsService service;
    private Account account;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        positions = mock(PositionRepository.class);
        quotes = mock(QuoteRepository.class);
        exchangeRates = mock(ExchangeRateRepository.class);
        freshness = mock(MarketDataFreshness.class);
        service = new WalletPositionsService(accounts, positions, quotes, exchangeRates, freshness);
        account = Account.create(UUID.randomUUID(), "Investor", "52998224725",
                "wallet@example.com", "hash", new BigDecimal("1000.00"), QUOTED_AT);
        when(accounts.findByIdAndStatus(account.getId(),
                com.projeto.gestao.domain.model.AccountStatus.ACTIVE))
                .thenReturn(Optional.of(account));
    }

    @Test
    void rejectsMissingAndInactiveAccounts() {
        assertThatThrownBy(() -> service.snapshot(null))
                .isInstanceOf(AuthenticationException.class);

        when(accounts.findByIdAndStatus(account.getId(),
                com.projeto.gestao.domain.model.AccountStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.snapshot(account.getId()))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void returnsBrlPositionAndMarksStaleQuote() {
        Position position = position(Currency.BRL, 2, "16.00");
        Quote quote = quote(Currency.BRL, "10.00", true);
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L))
                .thenReturn(List.of(position));
        when(quotes.findById(position.getAsset().getId())).thenReturn(Optional.of(quote));

        WalletPositionsSnapshot snapshot = service.snapshot(account.getId());
        WalletPositionView view = snapshot.positions().get(0);

        assertThat(snapshot.availableBalance()).isEqualByComparingTo("1000.00");
        assertThat(view.quotePrice()).isEqualByComparingTo("10.00");
        assertThat(view.quotePriceBrl()).isEqualByComparingTo("10.00");
        assertThat(view.marketValueBrl()).isEqualByComparingTo("20.00");
        assertThat(view.unrealizedResultBrl()).isEqualByComparingTo("4.00");
        assertThat(view.quoteStale()).isTrue();
        assertThat(view.usdBrlRate()).isNull();
        assertThat(view.exchangeRateStale()).isNull();
    }

    @Test
    void convertsUsdPositionAndMarksStaleExchangeRateFromFreshness() {
        Position position = position(Currency.USD, 3, "12.00");
        Quote quote = quote(Currency.USD, "4.00", false);
        ExchangeRate rate = rate("5.00", false);
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L))
                .thenReturn(List.of(position));
        when(quotes.findById(position.getAsset().getId())).thenReturn(Optional.of(quote));
        when(exchangeRates.findById("USD/BRL")).thenReturn(Optional.of(rate));
        when(freshness.quoteIsStale(any(Instant.class))).thenReturn(false);
        when(freshness.exchangeRateIsStale(any(Instant.class))).thenReturn(true);

        WalletPositionView view = service.snapshot(account.getId()).positions().get(0);

        assertThat(view.quotePrice()).isEqualByComparingTo("4.00");
        assertThat(view.quotePriceBrl()).isEqualByComparingTo("20.00");
        assertThat(view.marketValueBrl()).isEqualByComparingTo("60.00");
        assertThat(view.unrealizedResultBrl()).isEqualByComparingTo("48.00");
        assertThat(view.quoteStale()).isFalse();
        assertThat(view.usdBrlRate()).isEqualByComparingTo("5.00");
        assertThat(view.exchangeRateStale()).isTrue();
    }

    @Test
    void leavesValuationUnavailableWhenQuoteOrExchangeRateCannotBeUsed() {
        Position brl = position(Currency.BRL, 2, "16.00");
        Position usd = position(Currency.USD, 3, "12.00");
        Quote invalidBrlQuote = quote(Currency.USD, "10.00", false);
        Quote validUsdQuote = quote(Currency.USD, "4.00", false);
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L))
                .thenReturn(List.of(brl, usd));
        when(quotes.findById(brl.getAsset().getId())).thenReturn(Optional.of(invalidBrlQuote));
        when(quotes.findById(usd.getAsset().getId())).thenReturn(Optional.of(validUsdQuote));
        when(exchangeRates.findById("USD/BRL")).thenReturn(Optional.empty());
        when(freshness.quoteIsStale(any(Instant.class))).thenReturn(true);

        List<WalletPositionView> views = service.snapshot(account.getId()).positions();

        assertThat(views).hasSize(2);
        assertThat(views.get(0).quotePrice()).isNull();
        assertThat(views.get(0).quotePriceBrl()).isNull();
        assertThat(views.get(0).marketValueBrl()).isNull();
        assertThat(views.get(0).unrealizedResultBrl()).isNull();
        assertThat(views.get(0).quoteStale()).isFalse();
        assertThat(views.get(1).quotePrice()).isEqualByComparingTo("4.00");
        assertThat(views.get(1).quotePriceBrl()).isNull();
        assertThat(views.get(1).marketValueBrl()).isNull();
        assertThat(views.get(1).exchangeRateStale()).isNull();
    }

    @Test
    void leavesValuationUnavailableWhenQuoteIsMissing() {
        Position position = position(Currency.BRL, 1, "8.00");
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L))
                .thenReturn(List.of(position));
        when(quotes.findById(position.getAsset().getId())).thenReturn(Optional.empty());

        WalletPositionView view = service.snapshot(account.getId()).positions().get(0);

        assertThat(view.quotePrice()).isNull();
        assertThat(view.quotePriceBrl()).isNull();
        assertThat(view.marketValueBrl()).isNull();
        assertThat(view.unrealizedResultBrl()).isNull();
        assertThat(view.quoteQuotedAt()).isNull();
        assertThat(view.quoteStale()).isFalse();
    }

    private Position position(Currency currency, long quantity, String totalCost) {
        Asset asset = mock(Asset.class);
        UUID assetId = UUID.randomUUID();
        when(asset.getId()).thenReturn(assetId);
        when(asset.getTicker()).thenReturn(currency == Currency.USD ? "AAPL" : "PETR4");
        when(asset.getName()).thenReturn(currency == Currency.USD ? "Apple" : "Petrobras");
        when(asset.getMarket()).thenReturn(currency == Currency.USD ? Market.US : Market.BR);
        when(asset.getCurrency()).thenReturn(currency);

        Broker broker = mock(Broker.class);
        when(broker.getTradeName()).thenReturn("Corretora");
        AccountBroker association = mock(AccountBroker.class);
        when(association.getId()).thenReturn(UUID.randomUUID());
        when(association.getBroker()).thenReturn(broker);

        Position position = mock(Position.class);
        when(position.getAsset()).thenReturn(asset);
        when(position.getAccountBroker()).thenReturn(association);
        when(position.getQuantity()).thenReturn(quantity);
        when(position.getAveragePrice()).thenReturn(new BigDecimal(totalCost)
                .divide(BigDecimal.valueOf(quantity)));
        when(position.getTotalCost()).thenReturn(new BigDecimal(totalCost));
        return position;
    }

    private Quote quote(Currency currency, String price, boolean stale) {
        Quote quote = mock(Quote.class);
        when(quote.getPrice()).thenReturn(new BigDecimal(price));
        when(quote.getCurrency()).thenReturn(currency);
        when(quote.getQuotedAt()).thenReturn(QUOTED_AT);
        when(quote.isStale()).thenReturn(stale);
        return quote;
    }

    private ExchangeRate rate(String value, boolean stale) {
        ExchangeRate rate = mock(ExchangeRate.class);
        when(rate.getRate()).thenReturn(new BigDecimal(value));
        when(rate.getQuotedAt()).thenReturn(QUOTED_AT);
        when(rate.isStale()).thenReturn(stale);
        return rate;
    }
}
