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
                .andExpect(jsonPath("$.positions").isEmpty())
                .andExpect(jsonPath("$.positionsMarketValueBrl").value(0.00))
                .andExpect(jsonPath("$.patrimonyBrl").value(10000.00))
                .andExpect(jsonPath("$.realizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.unrealizedResultBrl").value(0.00))
                .andExpect(jsonPath("$.totalResultBrl").value(0.00))
                .andExpect(jsonPath("$.exchangeRate").doesNotExist())
                .andExpect(jsonPath("$.warnings").isEmpty());
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

    private void addPosition(Account account, String ticker, Market market, Currency currency,
            long quantity, String averagePriceBrl, String quotePrice, boolean stale) {
        transactions.executeWithoutResult(status -> {
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
