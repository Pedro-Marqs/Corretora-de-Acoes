package com.projeto.gestao.service;

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
public class SaleService {
    private final AccountRepository accounts;
    private final AccountBrokerRepository accountBrokers;
    private final AssetRepository assets;
    private final AssetCatalogService market;
    private final SaleTransactionService transactions;

    public SaleService(AccountRepository accounts, AccountBrokerRepository accountBrokers,
            AssetRepository assets, AssetCatalogService market,
            SaleTransactionService transactions) {
        this.accounts = accounts;
        this.accountBrokers = accountBrokers;
        this.assets = assets;
        this.market = market;
        this.transactions = transactions;
    }

    public SaleResult sell(UUID accountId, UUID assetId, UUID brokerAssociationId, long quantity) {
        if (accountId == null) throw new AuthenticationException();
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        accounts.findByIdAndStatus(accountId, AccountStatus.ACTIVE)
                .orElseThrow(AuthenticationException::new);
        Asset asset = assets.findByIdAndStatus(assetId, AssetStatus.ACTIVE)
                .orElseThrow(() -> new MarketDataUnavailableException("Ativo indisponível."));
        accountBrokers.findByIdAndAccountIdAndStatus(
                brokerAssociationId, accountId, AssociationStatus.ACTIVE)
                .orElseThrow(AuthorizationException::new);

        AssetPriceView price = market.find(asset.getTicker(), asset.getMarket());
        FinancialAmount originalPrice = new FinancialAmount(price.originalPrice());
        FinancialAmount unitPriceBrl = asset.getMarket() == Market.US
                ? originalPrice.convertUsdToBrl(price.usdBrlRate()) : originalPrice;
        SaleQuote quote = new SaleQuote(asset.getId(), price.ticker(), price.market(),
                price.currency(), originalPrice.value(), unitPriceBrl.value(), price.quoteSource(),
                price.quoteQuotedAt(), price.quoteStale(), price.usdBrlRate(),
                price.exchangeRateSource(), price.exchangeRateQuotedAt(),
                price.exchangeRateStale());
        return transactions.sell(accountId, brokerAssociationId, quantity, quote);
    }
}
