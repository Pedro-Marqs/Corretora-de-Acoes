package com.projeto.gestao.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.AuthorizationException;
import com.projeto.gestao.api.exception.MarketDataUnavailableException;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.AssetStatus;
import com.projeto.gestao.domain.model.AssociationStatus;
import com.projeto.gestao.domain.model.FinancialAmount;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService {
    private final AccountRepository accounts;
    private final AccountBrokerRepository accountBrokers;
    private final AssetRepository assets;
    private final AssetCatalogService market;
    private final PurchaseTransactionService transactions;
    private final Clock clock;

    public PurchaseService(AccountRepository accounts, AccountBrokerRepository accountBrokers,
            AssetRepository assets, AssetCatalogService market,
            PurchaseTransactionService transactions) {
        this(accounts, accountBrokers, assets, market, transactions, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PurchaseService(AccountRepository accounts, AccountBrokerRepository accountBrokers,
            AssetRepository assets, AssetCatalogService market,
            PurchaseTransactionService transactions, Clock clock) {
        this.accounts = accounts;
        this.accountBrokers = accountBrokers;
        this.assets = assets;
        this.market = market;
        this.transactions = transactions;
        this.clock = clock;
    }

    public PurchaseResult purchase(UUID accountId, UUID assetId, UUID brokerAssociationId,
            long quantity) {
        return purchase(accountId, assetId, brokerAssociationId, quantity, null, null);
    }

    public PurchaseResult purchase(UUID accountId, UUID assetId, UUID brokerAssociationId,
            long quantity, BigDecimal requestedUnitPrice, OffsetDateTime occurredAt) {
        if (accountId == null) throw new AuthenticationException();
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        validatePrice(requestedUnitPrice);
        OffsetDateTime effectiveOccurredAt = effectiveOccurredAt(occurredAt);
        accounts.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
        Asset asset = assets.findByIdAndStatus(assetId, AssetStatus.ACTIVE)
                .orElseThrow(() -> new MarketDataUnavailableException("Ativo indisponível."));
        accountBrokers.findByIdAndAccountIdAndStatus(
                brokerAssociationId, accountId, AssociationStatus.ACTIVE)
                .orElseThrow(AuthorizationException::new);

        AssetPriceView price = market.find(asset.getTicker(), asset.getMarket());
        FinancialAmount originalPrice = new FinancialAmount(price.originalPrice());
        BigDecimal confirmedOriginalPrice = requestedUnitPrice == null
                ? originalPrice.value() : requestedUnitPrice.setScale(2, java.math.RoundingMode.HALF_UP);
        FinancialAmount confirmedNative = new FinancialAmount(confirmedOriginalPrice);
        FinancialAmount unitPriceBrl = asset.getMarket() == Market.US
                ? confirmedNative.convertUsdToBrl(price.usdBrlRate()) : confirmedNative;
        PurchaseQuote quote = new PurchaseQuote(asset.getId(), price.ticker(), price.market(),
                price.currency(), originalPrice.value(), unitPriceBrl.value(), price.quoteSource(),
                price.quoteQuotedAt(), price.quoteStale(), price.usdBrlRate(),
                price.exchangeRateSource(), price.exchangeRateQuotedAt(),
                price.exchangeRateStale(), confirmedOriginalPrice);
        if (requestedUnitPrice == null && occurredAt == null) {
            return transactions.purchase(accountId, brokerAssociationId, quantity, quote);
        }
        return transactions.purchase(accountId, brokerAssociationId, quantity, quote, effectiveOccurredAt);
    }

    private static void validatePrice(BigDecimal price) {
        if (price != null && (price.signum() <= 0
                || price.setScale(2, java.math.RoundingMode.HALF_UP).signum() <= 0)) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }

    private OffsetDateTime effectiveOccurredAt(OffsetDateTime occurredAt) {
        OffsetDateTime now = clock == null ? OffsetDateTime.now() : OffsetDateTime.now(clock);
        if (occurredAt != null && occurredAt.isAfter(now)) {
            throw new IllegalArgumentException("Operation instant cannot be in the future");
        }
        return occurredAt == null ? now : occurredAt;
    }
}
