package com.projeto.gestao.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.BusinessRuleException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.AssetStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.FinancialAmount;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.PositionFinancialCalculator;
import com.projeto.gestao.domain.model.PositionQuantity;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleTransactionService {
    private final AccountRepository accounts;
    private final AccountBrokerRepository accountBrokers;
    private final AssetRepository assets;
    private final PositionRepository positions;
    private final QuoteRepository quotes;
    private final ExchangeRateRepository rates;
    private final FinancialHistoryService history;
    private final Clock clock;

    public SaleTransactionService(AccountRepository accounts,
            AccountBrokerRepository accountBrokers, AssetRepository assets,
            PositionRepository positions, QuoteRepository quotes,
            ExchangeRateRepository rates, FinancialHistoryService history, Clock clock) {
        this.accounts = accounts;
        this.accountBrokers = accountBrokers;
        this.assets = assets;
        this.positions = positions;
        this.quotes = quotes;
        this.rates = rates;
        this.history = history;
        this.clock = clock;
    }

    @Transactional
    public SaleResult sell(UUID accountId, UUID brokerAssociationId, long quantity,
            SaleQuote marketData) {
        Account account = accounts.findForUpdateByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
        AccountBroker association = accountBrokers.findForUpdateByIdAndAccountIdAndStatus(
                brokerAssociationId, accountId, AssociationStatus.ACTIVE)
                .orElseThrow(AuthorizationException::new);
        Asset asset = assets.findByIdAndStatus(marketData.assetId(), AssetStatus.ACTIVE)
                .orElseThrow(() -> new MarketDataUnavailableException("Ativo indisponível."));
        validateSnapshot(asset, marketData);

        Position position = positions.findForUpdateByAccountIdAndAccountBrokerIdAndAssetId(
                accountId, brokerAssociationId, asset.getId()).orElseThrow(
                        () -> BusinessRuleException.insufficientPosition(quantity, 0));
        long availableQuantity = position.getQuantity();
        if (quantity > availableQuantity) {
            throw BusinessRuleException.insufficientPosition(quantity, availableQuantity);
        }
        FinancialAmount unitPriceBrl = new FinancialAmount(marketData.unitPriceBrl());
        com.projeto.gestao.domain.model.SaleResult financial = PositionFinancialCalculator.sell(
                position.financialBalance(), PositionQuantity.positive(quantity), unitPriceBrl);
        position.apply(financial.position());
        account.credit(financial.proceeds().value());

        OffsetDateTime occurredAt = OffsetDateTime.now(clock);
        history.record(account, occurredAt, (id, owner, instant) -> Movement.sale(
                id, owner, asset.getTicker(), asset.getMarket(), marketData.originalPrice(),
                unitPriceBrl.value(), marketData.usdBrlRate(), quantity,
                financial.proceeds().value(), asset.getCurrency(),
                association.getBroker().getTradeName(), account.getBalance(),
                financial.realizedResult().value(), instant));

        return new SaleResult(asset.getId(), association.getId(), asset.getTicker(),
                asset.getMarket(), asset.getCurrency(), quantity, position.getQuantity(),
                marketData.originalPrice(), unitPriceBrl.value(), financial.proceeds().value(),
                financial.realizedResult().value(), position.getAveragePrice(),
                position.getTotalCost(), account.getBalance(), marketData.quoteSource(),
                marketData.quoteQuotedAt(), marketData.quoteStale(), marketData.usdBrlRate(),
                marketData.exchangeRateSource(), marketData.exchangeRateQuotedAt(),
                marketData.exchangeRateStale(), occurredAt);
    }

    private void validateSnapshot(Asset asset, SaleQuote marketData) {
        if (marketData == null || !asset.getId().equals(marketData.assetId())
                || !asset.getTicker().equalsIgnoreCase(marketData.ticker())
                || asset.getMarket() != marketData.market()
                || asset.getCurrency() != marketData.currency()
                || marketData.originalPrice() == null || marketData.originalPrice().signum() <= 0
                || marketData.unitPriceBrl() == null || marketData.unitPriceBrl().signum() <= 0) {
            throw new MarketDataUnavailableException("Cotação indisponível.");
        }
        var quote = quotes.findById(asset.getId()).orElseThrow(
                () -> new MarketDataUnavailableException("Cotação indisponível."));
        if (quote.getPrice().compareTo(marketData.originalPrice()) != 0
                || !quote.getQuotedAt().toInstant().equals(marketData.quoteQuotedAt())) {
            throw new MarketDataUnavailableException("Cotação indisponível.");
        }
        if (asset.getMarket() == Market.US) {
            var rate = rates.findById(PatrimonyCalculator.USD_BRL).orElseThrow(
                    () -> new MarketDataUnavailableException("Cotação USD/BRL indisponível."));
            if (marketData.usdBrlRate() == null
                    || rate.getRate().compareTo(marketData.usdBrlRate()) != 0
                    || !rate.getQuotedAt().toInstant().equals(marketData.exchangeRateQuotedAt())) {
                throw new MarketDataUnavailableException("Cotação USD/BRL indisponível.");
            }
            FinancialAmount expected = new FinancialAmount(marketData.originalPrice())
                    .convertUsdToBrl(marketData.usdBrlRate());
            if (expected.value().compareTo(marketData.unitPriceBrl()) != 0) {
                throw new MarketDataUnavailableException("Cotação USD/BRL indisponível.");
            }
        } else if (new FinancialAmount(marketData.originalPrice()).value()
                .compareTo(marketData.unitPriceBrl()) != 0) {
            throw new MarketDataUnavailableException("Cotação indisponível.");
        }
    }
}
