package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PositionRepository;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.JwtTokenService;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminFinancialIsolationTests {
    private static final String ADMIN_PASSWORD = "Senha Admin Isolamento 1!";
    private static final String INVESTOR_PASSWORD = "Senha Investidor Isolamento 1!";
    private static final String ADMIN_SECRET = testSecret();
    private static final String INVESTOR_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountBrokerRepository accountBrokerRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private PositionRepository positionRepository;
    @Autowired private AdminJwtTokenService adminJwtTokenService;
    @Autowired private JwtTokenService investorJwtTokenService;

    private UUID adminId;
    private UUID accountId;
    private String investorEmail;

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "true");
        registry.add("app.security.jwt.issuer", () -> "corretora-investor-isolation");
        registry.add("app.security.jwt.audience", () -> "corretora-investors-isolation");
        registry.add("app.security.jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.jwt.secret", () -> INVESTOR_SECRET);
        registry.add("app.security.admin-jwt.enabled", () -> "true");
        registry.add("app.security.admin-jwt.issuer", () -> "corretora-admin-isolation");
        registry.add("app.security.admin-jwt.audience", () -> "corretora-admin-api-isolation");
        registry.add("app.security.admin-jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.admin-jwt.refresh-token-ttl", () -> "PT8H");
        registry.add("app.security.admin-jwt.secret", () -> ADMIN_SECRET);
    }

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        investorEmail = "investor-" + accountId + "@example.com";
        adminUserRepository.saveAndFlush(AdminUser.create(adminId,
                "admin-" + adminId + "@example.com", passwordEncoder.encode(ADMIN_PASSWORD),
                OffsetDateTime.now()));
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Isolado", "52998224725",
                investorEmail, passwordEncoder.encode(INVESTOR_PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.now()));
    }

    @Test
    void validInvestorBearerReachesOnlyItsFinancialController() throws Exception {
        String token = investorJwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get("/api/accounts/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Investidor Isolado"));
    }

    @Test
    void adminBearerIsRejectedByFinancialChainBeforeController() throws Exception {
        String token = adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();

        mockMvc.perform(get("/api/wallet").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
    }

    @ParameterizedTest(name = "ADMIN is rejected before {0}")
    @MethodSource("adminFinancialRequests")
    void adminBearerIsRejectedForEveryPrivateFinancialMethod(
            String endpoint, MockHttpServletRequestBuilder request) throws Exception {
        String token = adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();
        Account before = accountRepository.findById(accountId).orElseThrow();
        long movementsBefore = movementRepository.count();
        long positionsBefore = positionRepository.count();
        long associationsBefore = accountBrokerRepository.count();

        mockMvc.perform(request.header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        Account after = accountRepository.findById(accountId).orElseThrow();
        assertThat(after.getEmail()).as(endpoint).isEqualTo(before.getEmail());
        assertThat(after.getPasswordHash()).as(endpoint).isEqualTo(before.getPasswordHash());
        assertThat(after.getBalance()).as(endpoint)
                .isEqualByComparingTo(before.getBalance());
        assertThat(movementRepository.count()).as(endpoint).isEqualTo(movementsBefore);
        assertThat(positionRepository.count()).as(endpoint).isEqualTo(positionsBefore);
        assertThat(accountBrokerRepository.count()).as(endpoint).isEqualTo(associationsBefore);
    }

    @Test
    void investorBearerIsRejectedByAdministrativeChainBeforeController() throws Exception {
        String token = investorJwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get("/api/admin/investors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incompatibleAuthenticatedPrincipalReturns403InsteadOf500() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AdminPrincipal(adminId), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get("/api/accounts/me").with(authentication(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @ParameterizedTest(name = "incompatible principal is rejected by {0}")
    @MethodSource("financialReadPaths")
    void incompatiblePrincipalIsRejectedByEveryFinancialRead(String endpoint) throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AdminPrincipal(adminId), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        mockMvc.perform(get(endpoint).with(authentication(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @Test
    void incompatiblePrincipalCannotMutateFinancialState() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AdminPrincipal(adminId), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        long movementsBefore = movementRepository.count();

        mockMvc.perform(post("/api/wallet/deposits")
                        .with(authentication(authentication))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 100.00))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));

        assertThat(accountRepository.findById(accountId).orElseThrow().getBalance())
                .isEqualByComparingTo("10000.00");
        assertThat(movementRepository.count()).isEqualTo(movementsBefore);
    }

    @Test
    void legacySessionRemainsFinancialOnlyAndCannotFallbackIntoAdminChain() throws Exception {
        Cookie session = loginAsInvestor();

        mockMvc.perform(get("/api/accounts/me").cookie(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/investors").cookie(session))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/investors").cookie(session)
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousAccessToBothPrivateAreasReturns401() throws Exception {
        mockMvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/investors")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "investor reaches {0}")
    @MethodSource("investorFinancialReadPaths")
    void validInvestorBearerReachesAuthorizedFinancialReads(String endpoint) throws Exception {
        String token = investorJwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private static Stream<Arguments> adminFinancialRequests() {
        String randomAsset = UUID.randomUUID().toString();
        String randomBroker = UUID.randomUUID().toString();
        return Stream.of(
                Arguments.of("AccountController.me", get("/api/accounts/me")),
                Arguments.of("AccountController.changeEmail", jsonRequest(
                        patch("/api/accounts/me/email"),
                        "{\"newEmail\":\"changed@example.com\",\"currentPassword\":\"Senha Admin Isolamento 1!\"}")),
                Arguments.of("AccountController.changePassword", jsonRequest(
                        patch("/api/accounts/me/password"),
                        "{\"currentPassword\":\"Senha Admin Isolamento 1!\",\"newPassword\":\"Nova Senha 1!\"}")),
                Arguments.of("AccountController.delete", jsonRequest(
                        delete("/api/accounts/me"),
                        "{\"email\":\"investor@example.com\",\"password\":\"Senha Admin Isolamento 1!\",\"confirmation\":\"Excluir\"}")),
                Arguments.of("AssetController.search", get("/api/assets/search")
                        .param("ticker", "PETR4").param("market", "BR")),
                Arguments.of("BrokerController.lookup", get("/api/brokers/search")
                        .param("cnpj", "02332886000104")),
                Arguments.of("BrokerController.associate", jsonRequest(
                        post("/api/brokers"), "{\"cnpj\":\"02332886000104\"}")),
                Arguments.of("BrokerController.list", get("/api/brokers")),
                Arguments.of("BrokerController.remove", delete("/api/brokers/" + randomBroker)
                        .with(csrf())),
                Arguments.of("DashboardController.dashboard", get("/api/dashboard")
                        .param("period", "4W")),
                Arguments.of("HistoryController.history", get("/api/history").param("page", "0")),
                Arguments.of("WalletController.positions", get("/api/wallet/positions")),
                Arguments.of("WalletController.balance", get("/api/wallet")),
                Arguments.of("WalletController.deposit", jsonRequest(
                        post("/api/wallet/deposits"), "{\"amount\":100.00}")),
                Arguments.of("WalletController.withdraw", jsonRequest(
                        post("/api/wallet/withdrawals"), "{\"amount\":10.00}")),
                Arguments.of("WalletController.purchase", jsonRequest(
                        post("/api/wallet/purchases"), "{\"assetId\":\"" + randomAsset
                                + "\",\"brokerId\":\"" + randomBroker
                                + "\",\"quantity\":1,\"unitPrice\":10.00}")),
                Arguments.of("WalletController.sale", jsonRequest(
                        post("/api/wallet/sales"), "{\"assetId\":\"" + randomAsset
                                + "\",\"brokerId\":\"" + randomBroker
                                + "\",\"quantity\":1,\"unitPrice\":10.00}")),
                Arguments.of("WalletController.transfer", jsonRequest(
                        post("/api/wallet/transfers"), "{\"originBrokerId\":\"" + randomBroker
                                + "\",\"destinationBrokerId\":\"" + UUID.randomUUID()
                                + "\",\"assetId\":\"" + randomAsset + "\",\"quantity\":1}")));
    }

    private static Stream<String> financialReadPaths() {
        return Stream.of(
                "/api/accounts/me",
                "/api/assets/search?ticker=PETR4&market=BR",
                "/api/brokers/search?cnpj=02332886000104",
                "/api/brokers",
                "/api/dashboard?period=4W",
                "/api/history?page=0",
                "/api/wallet/positions",
                "/api/wallet");
    }

    private static Stream<String> investorFinancialReadPaths() {
        return Stream.of(
                "/api/accounts/me",
                "/api/brokers",
                "/api/dashboard?period=4W",
                "/api/history?page=0",
                "/api/wallet/positions",
                "/api/wallet");
    }

    private static MockHttpServletRequestBuilder jsonRequest(
            MockHttpServletRequestBuilder request, String body) {
        return request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Cookie loginAsInvestor() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", investorEmail, "password", INVESTOR_PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();
        return result.getResponse().getCookie("SESSION");
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

}
