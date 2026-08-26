package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatrimonyCalculatorTests {
    private final PositionRepository positions = mock(PositionRepository.class);
    private final QuoteRepository quotes = mock(QuoteRepository.class);
    private final ExchangeRateRepository rates = mock(ExchangeRateRepository.class);
    private final PatrimonyCalculator calculator = new PatrimonyCalculator(positions, quotes, rates);
    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.create(UUID.randomUUID(), "Test", "52998224725", "test@example.com",
                "hash", new BigDecimal("100.00"), OffsetDateTime.now());
    }

    @Test
    void calculatesCashOnlyPatrimony() {
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L)).thenReturn(List.of());
        PatrimonyCalculation result = calculator.calculate(account);
        assertThat(result.balanceBrl()).isEqualByComparingTo("100.00");
        assertThat(result.positionsValueBrl()).isEqualByComparingTo("0.00");
        assertThat(result.usdBrlRate()).isNull();
        assertThat(result.patrimonyBrl()).isEqualByComparingTo("100.00");
    }

    @Test
    void valuesBrlAndUsdPositionsUsingQuotesAndExchangeRate() {
        Position br = position(Currency.BRL, 2, new BigDecimal("10.00"));
        Position us = position(Currency.USD, 3, new BigDecimal("4.00"));
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L))
                .thenReturn(List.of(br, us));
        ExchangeRate exchangeRate = rate("5.00");
        when(rates.findById(PatrimonyCalculator.USD_BRL)).thenReturn(Optional.of(exchangeRate));

        PatrimonyCalculation result = calculator.calculate(account);

        assertThat(result.positionsValueBrl()).isEqualByComparingTo("80.00");
        assertThat(result.usdBrlRate()).isEqualByComparingTo("5.00");
        assertThat(result.patrimonyBrl()).isEqualByComparingTo("180.00");
    }

    @Test
    void failsWhenRequiredQuoteOrExchangeRateIsMissing() {
        Position br = positionWithoutQuote(Currency.BRL, 1);
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L)).thenReturn(List.of(br));
        assertThatThrownBy(() -> calculator.calculate(account)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing quote");

        Position us = position(Currency.USD, 1, BigDecimal.TEN);
        when(positions.findByAccountIdAndQuantityGreaterThan(account.getId(), 0L)).thenReturn(List.of(us));
        when(rates.findById(PatrimonyCalculator.USD_BRL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> calculator.calculate(account)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exchange rate");
    }

    private Position position(Currency currency, long quantity, BigDecimal price) {
        Position position = positionWithoutQuote(currency, quantity);
        Quote quote = mock(Quote.class);
        when(quote.getCurrency()).thenReturn(currency);
        when(quote.getPrice()).thenReturn(price);
        when(quotes.findById(position.getAsset().getId())).thenReturn(Optional.of(quote));
        return position;
    }

    private Position positionWithoutQuote(Currency currency, long quantity) {
        Asset asset = mock(Asset.class);
        when(asset.getId()).thenReturn(UUID.randomUUID());
        when(asset.getTicker()).thenReturn(currency.name());
        when(asset.getCurrency()).thenReturn(currency);
        Position position = mock(Position.class);
        when(position.getAsset()).thenReturn(asset);
        when(position.getQuantity()).thenReturn(quantity);
        return position;
    }

    private ExchangeRate rate(String value) {
        ExchangeRate rate = mock(ExchangeRate.class);
        when(rate.getRate()).thenReturn(new BigDecimal(value));
        return rate;
    }
}
