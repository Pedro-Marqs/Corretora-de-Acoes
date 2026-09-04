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
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletControllerTests {
    private static final String PASSWORD = "Senha Forte 1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private PatrimonialPointRepository patrimonialPointRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ExchangeRateRepository exchangeRateRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private AccountBrokerRepository accountBrokerRepository;
    @Autowired private BrokerRepository brokerRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private BrazilMarketDataPort brazil;

    private Account first;
    private Account second;

    @BeforeEach
    void createAccounts() {
        cleanup();
        first = saveAccount("52998224725", "first-wallet@example.com");
        second = saveAccount("11144477735", "second-wallet@example.com");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        patrimonialPointRepository.deleteAll();
        movementRepository.deleteAll();
        positionRepository.deleteAll();
        quoteRepository.deleteAll();
        exchangeRateRepository.deleteAll();
        accountBrokerRepository.deleteAll();
        brokerRepository.deleteAll();
        assetRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void purchaseUsesSessionAccountAndBackendPriceWhileIgnoringInjectedFields() throws Exception {
        Cookie session = login(first.getEmail());
        Asset asset = assetRepository.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        Broker broker = brokerRepository.save(Broker.create(UUID.randomUUID(), "02332886000104",
                "XP INVESTIMENTOS SA", "XP", "ATIVA", "CTVM", "01001000", "Rua A", "1",
                null, "Centro", "São Paulo", "SP", OffsetDateTime.now()));
        AccountBroker association = accountBrokerRepository.save(AccountBroker.create(
                UUID.randomUUID(), first, broker, OffsetDateTime.now()));
        org.mockito.Mockito.when(brazil.findQuote("PETR4")).thenReturn(new MarketQuote(
                "PETR4", "Petrobras", Market.BR, Currency.BRL, new BigDecimal("25.00"),
                java.time.Instant.parse("2026-09-03T13:00:00Z"),
                java.time.Instant.parse("2026-09-03T13:00:01Z"), "Brapi"));
        MvcResult search = mockMvc.perform(get("/api/assets/search")
                        .param("ticker", "PETR4").param("market", "BR").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(asset.getId().toString()))
                .andReturn();
        UUID searchedAssetId = UUID.fromString(objectMapper
                .readTree(search.getResponse().getContentAsString()).path("assetId").asText());
        CsrfCredentials csrf = csrf();

        mockMvc.perform(post("/api/wallet/purchases").cookie(session, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assetId", searchedAssetId, "brokerId", association.getId(),
                                "quantity", 2, "accountId", second.getId(), "price", 0.01))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseAmountBrl").value(50.00))
                .andExpect(jsonPath("$.remainingBalanceBrl").value(9950.00))
                .andExpect(jsonPath("$.accountId").doesNotExist())
                .andExpect(jsonPath("$.price").doesNotExist());
        assertThat(accountRepository.findById(second.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void purchaseValidatesContractAndRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/wallet/purchases").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        Cookie session = login(first.getEmail());
        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/wallet/purchases").cookie(session, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
        mockMvc.perform(post("/api/wallet/purchases").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertThat(movementRepository.count()).isZero();
    }

    @Test
    void saleUsesSessionAccountAndBackendPriceWhileIgnoringInjectedFields() throws Exception {
        Cookie session = login(first.getEmail());
        Asset asset = assetRepository.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        Broker broker = brokerRepository.save(Broker.create(UUID.randomUUID(), "02332886000104",
                "XP INVESTIMENTOS SA", "XP", "ATIVA", "CTVM", "01001000", "Rua A", "1",
                null, "Centro", "São Paulo", "SP", OffsetDateTime.now()));
        AccountBroker association = accountBrokerRepository.save(AccountBroker.create(
                UUID.randomUUID(), first, broker, OffsetDateTime.now()));
        org.mockito.Mockito.when(brazil.findQuote("PETR4")).thenReturn(new MarketQuote(
                "PETR4", "Petrobras", Market.BR, Currency.BRL, new BigDecimal("25.00"),
                java.time.Instant.parse("2026-09-03T13:00:00Z"),
                java.time.Instant.parse("2026-09-03T13:00:01Z"), "Brapi"), new MarketQuote(
                "PETR4", "Petrobras", Market.BR, Currency.BRL, new BigDecimal("30.00"),
                java.time.Instant.parse("2026-09-03T13:05:00Z"),
                java.time.Instant.parse("2026-09-03T13:05:01Z"), "Brapi"));
        CsrfCredentials purchaseCsrf = csrf();
        mockMvc.perform(post("/api/wallet/purchases").cookie(session, purchaseCsrf.cookie())
                .header("X-XSRF-TOKEN", purchaseCsrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("assetId", asset.getId(),
                        "brokerId", association.getId(), "quantity", 2))))
                .andExpect(status().isOk());
        CsrfCredentials saleCsrf = csrf();

        mockMvc.perform(post("/api/wallet/sales").cookie(session, saleCsrf.cookie())
                        .header("X-XSRF-TOKEN", saleCsrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assetId", asset.getId(), "brokerId", association.getId(),
                                "quantity", 1, "accountId", second.getId(), "price", 0.01))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleAmountBrl").value(30.00))
                .andExpect(jsonPath("$.realizedResultBrl").value(5.00))
                .andExpect(jsonPath("$.remainingBalanceBrl").value(9980.00))
                .andExpect(jsonPath("$.accountId").doesNotExist())
                .andExpect(jsonPath("$.price").doesNotExist());
        assertThat(accountRepository.findById(second.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void saleValidatesIntegerPositiveContractAndRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/wallet/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        CsrfCredentials anonymousCsrf = csrf();
        mockMvc.perform(post("/api/wallet/sales").cookie(anonymousCsrf.cookie())
                        .header("X-XSRF-TOKEN", anonymousCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"" + UUID.randomUUID()
                                + "\",\"brokerId\":\"" + UUID.randomUUID()
                                + "\",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
        Cookie session = login(first.getEmail());
        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/wallet/sales").cookie(session, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
        mockMvc.perform(post("/api/wallet/sales").cookie(session, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":\"" + UUID.randomUUID()
                                + "\",\"brokerId\":\"" + UUID.randomUUID()
                                + "\",\"quantity\":1.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(movementRepository.count()).isZero();
    }

    @Test
    void returnsInitialBalanceAndDepositsOnlyIntoSessionAccount() throws Exception {
        Cookie firstSession = login(first.getEmail());
        Cookie secondSession = login(second.getEmail());

        mockMvc.perform(get("/api/wallet").cookie(firstSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.00))
                .andExpect(jsonPath("$.accountId").doesNotExist());

        postDeposit(firstSession, Map.of("amount", 500.005, "accountId", second.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10500.01));

        mockMvc.perform(get("/api/wallet").cookie(secondSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.00));
        assertThat(accountRepository.findById(first.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10500.01");
        assertThat(accountRepository.findById(second.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
    }

    @Test
    void acceptsMinimumAndRejectsInvalidDepositsWithoutEffects() throws Exception {
        Cookie session = login(first.getEmail());
        postDeposit(session, Map.of("amount", 10.00))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10010.00));

        for (Object amount : new Object[] {0, -1, 9.999, "not-a-number"}) {
            postDeposit(session, Map.of("amount", amount))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        postDeposit(session, Map.of()).andExpect(status().isBadRequest());
        assertThat(accountRepository.findById(first.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10010.00");
        assertThat(movementRepository.count()).isEqualTo(1);
        assertThat(patrimonialPointRepository.count()).isEqualTo(1);
    }

    @Test
    void requiresAuthenticationAndCsrfForDeposit() throws Exception {
        mockMvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
        Cookie session = login(first.getEmail());
        mockMvc.perform(post("/api/wallet/deposits").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isForbidden());
        assertThat(accountRepository.findById(first.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
    }

    private Account saveAccount(String cpf, String email) {
        return accountRepository.saveAndFlush(Account.create(UUID.randomUUID(), "Investor", cpf,
                email, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-08-26T10:00:00-03:00")));
    }

    private Cookie login(String email) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");
    }

    private org.springframework.test.web.servlet.ResultActions postDeposit(
            Cookie session, Map<String, ?> body) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/wallet/deposits").cookie(session, csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private record CsrfCredentials(Cookie cookie, String token) { }
}
