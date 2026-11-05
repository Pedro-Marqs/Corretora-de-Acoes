package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Asset;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.CompanyRegistryPort;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AssetRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.JwtTokenService;
import com.projeto.gestao.support.ExternalRegistryTestStubs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtBearerFinancialAuthorizationTests {
    private static final String ISSUER = "https://corretora.test/financial";
    private static final String AUDIENCE = "corretora-investors";
    private static final String TEST_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBrokerRepository accountBrokerRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private BrokerRepository brokerRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;
    @MockitoBean private BrazilMarketDataPort brazil;
    @MockitoBean private CompanyRegistryPort companies;
    @MockitoBean private RegulatoryRegistryPort regulatoryRegistry;

    private Account first;
    private Account second;
    private AccountBroker firstAssociation;
    private AccountBroker secondAssociation;
    private Asset asset;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.issuer", () -> ISSUER);
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
        registry.add("app.security.jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void setUp() {
        ExternalRegistryTestStubs.active(companies, regulatoryRegistry);
        first = saveAccount("52998224725", "jwt-first@example.com");
        second = saveAccount("11144477735", "jwt-second@example.com");
        firstAssociation = saveAssociation(first, "02332886000104", "Primeira");
        secondAssociation = saveAssociation(second, "10270580000110", "Segunda");
        asset = assetRepository.save(new Asset("PETR4", "Petrobras", Market.BR, Currency.BRL));
        when(brazil.findQuote("PETR4")).thenReturn(new MarketQuote(
                "PETR4", "Petrobras", Market.BR, Currency.BRL, new BigDecimal("25.00"),
                Instant.parse("2026-09-03T13:00:00Z"),
                Instant.parse("2026-09-03T13:00:01Z"), "Brapi"));
    }

    @Test
    void bearerPurchaseUsesAuthenticatedInvestorAccount() throws Exception {
        String token = jwtTokenService.issue(new AccountPrincipal(first.getId())).tokenValue();

        mockMvc.perform(post("/api/wallet/purchases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assetId", asset.getId(), "brokerId", firstAssociation.getId(),
                                "quantity", 2, "accountId", second.getId(), "price", 0.01))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseAmountBrl").value(0.02))
                .andExpect(jsonPath("$.remainingBalanceBrl").value(9999.98));

        assertThat(accountRepository.findById(first.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("9999.98");
        assertThat(accountRepository.findById(second.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
        assertThat(movementRepository.findAll()).singleElement()
                .satisfies(movement -> assertThat(movement.getAccount().getId()).isEqualTo(first.getId()));
    }

    @Test
    void bearerCannotPurchaseThroughAnotherInvestorsBrokerAssociation() throws Exception {
        String token = jwtTokenService.issue(new AccountPrincipal(first.getId())).tokenValue();

        mockMvc.perform(post("/api/wallet/purchases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "assetId", asset.getId(), "brokerId", secondAssociation.getId(),
                                "quantity", 1, "price", 25.00))))
                .andExpect(status().isForbidden());

        assertThat(accountRepository.findById(first.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
        assertThat(movementRepository.count()).isZero();
    }

    private Account saveAccount(String cpf, String email) {
        return accountRepository.saveAndFlush(Account.create(UUID.randomUUID(), "Investor", cpf,
                email, passwordEncoder.encode("Senha Financeira 1!"), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-09-03T10:00:00-03:00")));
    }

    private AccountBroker saveAssociation(Account owner, String cnpj, String name) {
        Broker broker = brokerRepository.save(Broker.create(UUID.randomUUID(), cnpj, name + " SA",
                name, "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro",
                "São Paulo", "SP", OffsetDateTime.now()));
        return accountBrokerRepository.save(AccountBroker.create(
                UUID.randomUUID(), owner, broker, OffsetDateTime.now()));
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
