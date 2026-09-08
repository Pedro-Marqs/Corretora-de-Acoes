package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.ExchangeRate;
import com.projeto.gestao.domain.model.FinancialAmount;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.Position;
import com.projeto.gestao.domain.model.PositionBalance;
import com.projeto.gestao.domain.model.PositionQuantity;
import com.projeto.gestao.domain.model.Quote;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.ExchangeRateRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.repository.QuoteRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTests {
    private static final String PASSWORD = "Senha Forte 1!";
    private static final OffsetDateTime NOW = OffsetDateTime.now().withNano(0);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accounts;
    @Autowired private AssetRepository assets;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private PositionRepository positions;
    @Autowired private QuoteRepository quotes;
    @Autowired private ExchangeRateRepository exchangeRates;
    @Autowired private MovementRepository movements;
    @Autowired private PatrimonialPointRepository points;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        points.deleteAll();
        movements.deleteAll();
        positions.deleteAll();
        quotes.deleteAll();
        exchangeRates.deleteAll();
        associations.deleteAll();
        brokers.deleteAll();
        assets.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void rejectsMissingSessionWithUniformAuthenticationErrorAndNoDataLeak() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errorId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.positions").doesNotExist())
                .andExpect(jsonPath("$.patrimonyBrl").doesNotExist());
    }

    @Test
    void returnsEmptyDashboardForAccountWithoutInvestmentData() throws Exception {
        Account account = account("52998224725", "empty-dashboard@example.com", "10000");

        mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalanceBrl").value(10000.00))
                .andExpect(jsonPath("$.balanceShared").value(true))
                .andExpect(jsonPath("$.selectedBrokerAssociationId").doesNotExist())
                .andExpect(jsonPath("$.positions").isEmpty())
                .andExpect(jsonPath("$.positionsMarketValueBrl").value(0.00))
                .andExpect(jsonPath("$.patrimonyBrl").value(10000.00))
                .andExpect(jsonPath("$.realizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.unrealizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.totalResultBrl").value(0.00))
                .andExpect(jsonPath("$.exchangeRate").doesNotExist())
                .andExpect(jsonPath("$.distributions.byAsset").isEmpty())
                .andExpect(jsonPath("$.distributions.byBroker").isEmpty())
                .andExpect(jsonPath("$.distributions.byMarket").isEmpty())
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    void filtersSameAssetByActiveBrokerAndKeepsSharedBalance() throws Exception {
        Account account = account("28001238938", "filtered-dashboard@example.com", "4321");
        Asset asset = assets.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        AccountBroker first = associate(account, "First Broker");
        AccountBroker second = associate(account, "Second Broker");
        addPosition(account, first, asset, 10, "20", "25", false);
        addPosition(account, second, asset, 4, "20", "25", false);

        mockMvc.perform(get("/api/dashboard")
                        .param("brokerAssociationId", first.getId().toString())
                        .cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalanceBrl").value(4321.00))
                .andExpect(jsonPath("$.balanceShared").value(true))
                .andExpect(jsonPath("$.selectedBrokerAssociationId")
                        .value(first.getId().toString()))
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].brokerageName").value("First Broker"))
                .andExpect(jsonPath("$.positionsMarketValueBrl").value(250.00))
                .andExpect(jsonPath("$.patrimonyBrl").value(4571.00))
                .andExpect(jsonPath("$.distributions.byAsset.length()").value(1))
                .andExpect(jsonPath("$.distributions.byAsset[0].valueBrl").value(250.00))
                .andExpect(jsonPath("$.distributions.byBroker.length()").value(1))
                .andExpect(jsonPath("$.distributions.byBroker[0].identifier")
                        .value(first.getId().toString()))
                .andExpect(jsonPath("$.distributions.byMarket[0].valueBrl").value(250.00));
    }

    @Test
    void returnsBrlDistributionsWhoseDimensionsReconcileWithMarketValue() throws Exception {
        Account account = account("74415353000", "distributions-dashboard@example.com", "1000");
        AccountBroker first = addPosition(account, "PETR4", Market.BR, Currency.BRL,
                10, "20", "25", false);
        AccountBroker second = addPosition(account, "AAPL", Market.US, Currency.USD,
                2, "40", "10", false);
        exchangeRates.save(new ExchangeRate("USD/BRL", amount("5"), NOW, NOW, "cache"));

        MvcResult result = mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionsMarketValueBrl").value(350.00))
                .andExpect(jsonPath("$.distributions.byAsset.length()").value(2))
                .andExpect(jsonPath("$.distributions.byBroker.length()").value(2))
                .andExpect(jsonPath("$.distributions.byMarket.length()").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String dimension : new String[] {"byAsset", "byBroker", "byMarket"}) {
            BigDecimal total = BigDecimal.ZERO;
            for (JsonNode slice : body.path("distributions").path(dimension)) {
                total = total.add(slice.path("valueBrl").decimalValue());
            }
            assertThat(total).isEqualByComparingTo(body.path("positionsMarketValueBrl").decimalValue());
        }
        assertThat(body.path("distributions").path("byBroker").toString())
                .contains(first.getId().toString(), second.getId().toString());
    }

    @Test
    void restrictsRealizedResultsToSelectedBroker() throws Exception {
        Account account = account("65337751000", "results-dashboard@example.com", "1000");
        AccountBroker first = associate(account, "Results One");
        associate(account, "Results Two");
        movements.save(Movement.sale(UUID.randomUUID(), account, "PETR4", Market.BR,
                amount("25"), amount("25"), null, 2, amount("50"), Currency.BRL,
                "Results One", amount("1000"), amount("10"), NOW));
        movements.save(Movement.sale(UUID.randomUUID(), account, "VALE3", Market.BR,
                amount("30"), amount("30"), null, 1, amount("30"), Currency.BRL,
                "Results Two", amount("1000"), amount("-5"), NOW));

        mockMvc.perform(get("/api/dashboard")
                        .param("brokerAssociationId", first.getId().toString())
                        .cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizedResultBrl").value(10.00))
                .andExpect(jsonPath("$.unrealizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.totalResultBrl").value(10.00));
    }

    @Test
    void rejectsBrokerFromAnotherAccountOrInactiveBrokerWithoutLeakingData() throws Exception {
        Account owner = account("29550677002", "owner-dashboard@example.com", "1000");
        Account intruder = account("36687563006", "intruder-dashboard@example.com", "2000");
        AccountBroker foreign = addPosition(owner, "VALE3", Market.BR, Currency.BRL,
                2, "30", "40", false);
        AccountBroker inactive = associate(intruder, "Inactive Broker");
        transactions.executeWithoutResult(status -> {
            AccountBroker managed = associations.findById(inactive.getId()).orElseThrow();
            managed.inactivate(NOW);
            associations.save(managed);
        });

        for (UUID brokerId : new UUID[] {foreign.getId(), inactive.getId()}) {
            mockMvc.perform(get("/api/dashboard")
                            .param("brokerAssociationId", brokerId.toString())
                            .cookie(login(intruder.getEmail())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"))
                    .andExpect(jsonPath("$.positions").doesNotExist())
                    .andExpect(jsonPath("$.distributions").doesNotExist());
        }
    }

    @Test
    void filteredViewIgnoresUsdPositionsAndMissingRateFromAnotherBroker() throws Exception {
        Account account = account("71428793860", "filtered-rate-dashboard@example.com", "1000");
        AccountBroker brBroker = addPosition(account, "ITUB4", Market.BR, Currency.BRL,
                5, "20", "30", false);
        addPosition(account, "MSFT", Market.US, Currency.USD, 2, "40", "10", false);

        mockMvc.perform(get("/api/dashboard")
                        .param("brokerAssociationId", brBroker.getId().toString())
                        .cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].ticker").value("ITUB4"))
                .andExpect(jsonPath("$.exchangeRate").doesNotExist());
    }

    @Test
    void calculatesPatrimonyOfThreeThousandFiveHundred() throws Exception {
        Account account = account("11144477735", "patrimony-dashboard@example.com", "1000");
        addPosition(account, "PETR4", Market.BR, Currency.BRL, 100, "20", "25", false);

        mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionsMarketValueBrl").value(2500.00))
                .andExpect(jsonPath("$.patrimonyBrl").value(3500.00))
                .andExpect(jsonPath("$.unrealizedResultBrl").value(500.00))
                .andExpect(jsonPath("$.totalResultBrl").value(500.00));
    }

    @Test
    void convertsUsdPositionUsingFiveReaisExchangeRate() throws Exception {
        Account account = account("39053344705", "usd-dashboard@example.com", "1000");
        addPosition(account, "AAPL", Market.US, Currency.USD, 2, "40", "10", false);
        exchangeRates.save(new ExchangeRate("USD/BRL", amount("5"), NOW, NOW, "cache"));

        mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].marketValueBrl").value(100.00))
                .andExpect(jsonPath("$.positionsMarketValueBrl").value(100.00))
                .andExpect(jsonPath("$.exchangeRate.currencyPair").value("USD/BRL"))
                .andExpect(jsonPath("$.exchangeRate.rate").value(5.00))
                .andExpect(jsonPath("$.exchangeRate.quotedAt").exists());
    }

    @Test
    void rejectsUsdPositionWhenExchangeRateIsUnavailable() throws Exception {
        Account account = account("93541134780", "missing-rate-dashboard@example.com", "1000");
        addPosition(account, "MSFT", Market.US, Currency.USD, 2, "40", "10", false);

        mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_DEPENDENCY_ERROR"))
                .andExpect(jsonPath("$.errorId").exists())
                .andExpect(jsonPath("$.positions").doesNotExist())
                .andExpect(jsonPath("$.patrimonyBrl").doesNotExist());
    }

    @Test
    void preservesOldQuoteAndMarksItAsStale() throws Exception {
        Account account = account("01234567890", "stale-dashboard@example.com", "1000");
        addPosition(account, "VALE3", Market.BR, Currency.BRL, 10, "20", "25", true);

        mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].marketValueBrl").value(250.00))
                .andExpect(jsonPath("$.positions[0].quoteStale").value(true))
                .andExpect(jsonPath("$.positions[0].quoteQuotedAt").exists())
                .andExpect(jsonPath("$.warnings[0].type").value("STALE_QUOTE"))
                .andExpect(jsonPath("$.warnings[0].ticker").value("VALE3"));
    }

    @Test
    void isolatesAccountsAndDashboardQueriesHaveNoSideEffects() throws Exception {
        Account first = account("98765432100", "first-dashboard@example.com", "1000");
        Account second = account("15350946056", "second-dashboard@example.com", "2000");
        addPosition(first, "PETR4", Market.BR, Currency.BRL, 10, "20", "25", false);
        addPosition(second, "VALE3", Market.BR, Currency.BRL, 4, "30", "40", false);
        long movementCount = movements.count();
        long pointCount = points.count();

        mockMvc.perform(get("/api/dashboard").cookie(login(first.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'VALE3')]").isEmpty());
        mockMvc.perform(get("/api/dashboard").cookie(login(second.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].ticker").value("VALE3"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'PETR4')]").isEmpty());
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(points.count()).isEqualTo(pointCount);
    }

    @Test
    void depositRaisesBalanceAndPatrimonyButNotInvestmentResults() throws Exception {
        Account account = account("16899535009", "deposit-dashboard@example.com", "10000");
        account.credit(amount("500"));
        accounts.save(account);
        movements.save(Movement.deposit(UUID.randomUUID(), account, amount("500"),
                amount("10500"), NOW));

        mockMvc.perform(get("/api/dashboard").cookie(login(account.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalanceBrl").value(10500.00))
                .andExpect(jsonPath("$.patrimonyBrl").value(10500.00))
                .andExpect(jsonPath("$.realizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.unrealizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.totalResultBrl").value(0.00));
    }

    private Account account(String cpf, String email, String balance) {
        return accounts.save(Account.create(UUID.randomUUID(), "Investor", cpf, email,
                passwordEncoder.encode(PASSWORD), amount(balance), NOW));
    }

    private AccountBroker addPosition(Account account, String ticker, Market market, Currency currency,
            long quantity, String averagePriceBrl, String quotePrice, boolean stale) {
        return transactions.execute(status -> {
            Account managedAccount = accounts.findById(account.getId()).orElseThrow();
            Asset asset = assets.save(new Asset(ticker, ticker + " Company", market, currency));
            Broker broker = brokers.save(Broker.create(UUID.randomUUID(), randomCnpj(),
                    ticker + " Broker SA", ticker + " Broker", "ATIVA", "CTVM", "01001000",
                    "Rua A", "1", null, "Centro", "Sao Paulo", "SP", NOW));
            AccountBroker association = associations.save(AccountBroker.create(
                    UUID.randomUUID(), managedAccount, broker, NOW));
            FinancialAmount average = new FinancialAmount(amount(averagePriceBrl));
            positions.save(Position.create(UUID.randomUUID(), managedAccount, association, asset,
                    new PositionBalance(PositionQuantity.positive(quantity),
                            average.multiply(quantity), average)));
            Quote quote = new Quote(asset, amount(quotePrice), currency,
                    stale ? NOW.minusDays(2) : NOW, NOW, "cache");
            if (stale) {
                quote.markStale();
            }
            quotes.save(quote);
            return association;
        });
    }

    private AccountBroker associate(Account account, String tradeName) {
        return transactions.execute(status -> {
            Account managedAccount = accounts.findById(account.getId()).orElseThrow();
            Broker broker = brokers.save(Broker.create(UUID.randomUUID(), randomCnpj(),
                    tradeName + " SA", tradeName, "ATIVA", "CTVM", "01001000",
                    "Rua A", "1", null, "Centro", "Sao Paulo", "SP", NOW));
            return associations.save(AccountBroker.create(
                    UUID.randomUUID(), managedAccount, broker, NOW));
        });
    }

    private void addPosition(Account account, AccountBroker association, Asset asset,
            long quantity, String averagePriceBrl, String quotePrice, boolean stale) {
        transactions.executeWithoutResult(status -> {
            Account managedAccount = accounts.findById(account.getId()).orElseThrow();
            AccountBroker managedAssociation = associations.findById(association.getId()).orElseThrow();
            Asset managedAsset = assets.findById(asset.getId()).orElseThrow();
            FinancialAmount average = new FinancialAmount(amount(averagePriceBrl));
            positions.save(Position.create(UUID.randomUUID(), managedAccount, managedAssociation,
                    managedAsset, new PositionBalance(PositionQuantity.positive(quantity),
                            average.multiply(quantity), average)));
            Quote quote = quotes.findById(managedAsset.getId()).orElseGet(() ->
                    new Quote(managedAsset, amount(quotePrice), managedAsset.getCurrency(),
                            stale ? NOW.minusDays(2) : NOW, NOW, "cache"));
            if (stale) quote.markStale();
            quotes.save(quote);
        });
    }

    private String randomCnpj() {
        return String.format("%014d", Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 100000000000000L);
    }

    private Cookie login(String email) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private record CsrfCredentials(Cookie cookie, String token) { }
}
