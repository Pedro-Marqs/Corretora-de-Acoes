package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.springframework.stereotype.Component;

@Component
public class PatrimonyCalculator {
    static final String USD_BRL = "USD/BRL";

    private final PositionRepository positionRepository;
    private final QuoteRepository quoteRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public PatrimonyCalculator(PositionRepository positionRepository, QuoteRepository quoteRepository,
            ExchangeRateRepository exchangeRateRepository) {
        this.positionRepository = positionRepository;
        this.quoteRepository = quoteRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public PatrimonyCalculation calculate(Account account) {
        if (account == null || account.getId() == null || account.getBalance() == null) {
            throw new IllegalArgumentException("Account with balance is required");
        }
        BigDecimal balance = normalize(account.getBalance());
        List<Position> positions = positionRepository
                .findByAccountIdAndQuantityGreaterThan(account.getId(), 0L);
        BigDecimal positionsValue = BigDecimal.ZERO.setScale(2);
        BigDecimal usdBrlRate = null;

        for (Position position : positions) {
            Quote quote = quoteRepository.findById(position.getAsset().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing quote for asset " + position.getAsset().getTicker()));
            Currency assetCurrency = position.getAsset().getCurrency();
            if (quote.getCurrency() != assetCurrency) {
                throw new IllegalStateException("Quote currency differs from asset currency");
            }
            BigDecimal value = quote.getPrice().multiply(BigDecimal.valueOf(position.getQuantity()));
            if (assetCurrency == Currency.USD) {
                if (usdBrlRate == null) {
                    ExchangeRate rate = exchangeRateRepository.findById(USD_BRL)
                            .orElseThrow(() -> new IllegalStateException("Missing USD/BRL exchange rate"));
                    usdBrlRate = positive(rate.getRate(), "USD/BRL exchange rate");
                }
                value = value.multiply(usdBrlRate);
            }
            positionsValue = positionsValue.add(normalize(positive(value, "Position value")));
        }
        positionsValue = normalize(positionsValue);
        return new PatrimonyCalculation(balance, positionsValue, usdBrlRate,
                normalize(balance.add(positionsValue)));
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalStateException(field + " must be positive");
        }
        return value;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
