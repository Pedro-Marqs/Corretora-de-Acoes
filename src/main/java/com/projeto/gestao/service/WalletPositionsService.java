package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletPositionsService {
    private static final String USD_BRL = "USD/BRL";
    private final AccountRepository accounts;
    private final PositionRepository positions;
    private final QuoteRepository quotes;
    private final ExchangeRateRepository exchangeRates;
    private final MarketDataFreshness freshness;

    public WalletPositionsService(AccountRepository accounts, PositionRepository positions,
            QuoteRepository quotes, ExchangeRateRepository exchangeRates,
            MarketDataFreshness freshness) {
        this.accounts = accounts;
        this.positions = positions;
        this.quotes = quotes;
        this.exchangeRates = exchangeRates;
        this.freshness = freshness;
    }

    @Transactional(readOnly = true)
    public WalletPositionsSnapshot snapshot(UUID accountId) {
        if (accountId == null) throw new AuthenticationException();
        Account account = accounts.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
        List<WalletPositionView> items = positions
                .findByAccountIdAndQuantityGreaterThan(accountId, 0L).stream()
                .map(this::view).toList();
        return new WalletPositionsSnapshot(account.getBalance(), items);
    }

    private WalletPositionView view(Position position) {
        Quote quote = quotes.findById(position.getAsset().getId()).orElse(null);
        ExchangeRate rate = position.getAsset().getCurrency() == Currency.USD
                ? exchangeRates.findById(USD_BRL).orElse(null) : null;
        boolean usableQuote = quote != null && quote.getPrice() != null
                && quote.getPrice().signum() > 0
                && quote.getCurrency() == position.getAsset().getCurrency();
        boolean usableRate = position.getAsset().getCurrency() == Currency.BRL
                || rate != null && rate.getRate() != null && rate.getRate().signum() > 0;
        BigDecimal priceBrl = usableQuote && usableRate ? normalize(
                position.getAsset().getCurrency() == Currency.USD
                        ? quote.getPrice().multiply(rate.getRate()) : quote.getPrice()) : null;
        BigDecimal marketValue = priceBrl == null ? null
                : normalize(priceBrl.multiply(BigDecimal.valueOf(position.getQuantity())));
        BigDecimal unrealized = marketValue == null ? null
                : normalize(marketValue.subtract(position.getTotalCost()));
        boolean quoteStale = usableQuote && (quote.isStale()
                || freshness.quoteIsStale(quote.getQuotedAt().toInstant()));
        Boolean rateStale = rate == null ? null : rate.isStale()
                || freshness.exchangeRateIsStale(rate.getQuotedAt().toInstant());
        return new WalletPositionView(position.getAsset().getId(), position.getAsset().getTicker(),
                position.getAsset().getName(), position.getAsset().getMarket(),
                position.getAsset().getCurrency(), position.getAccountBroker().getId(),
                position.getAccountBroker().getBroker().getTradeName(), position.getQuantity(),
                position.getAveragePrice(), usableQuote ? quote.getPrice() : null, priceBrl,
                marketValue, unrealized, usableQuote ? quote.getQuotedAt() : null, quoteStale,
                rate == null ? null : rate.getRate(), rate == null ? null : rate.getQuotedAt(), rateStale);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
