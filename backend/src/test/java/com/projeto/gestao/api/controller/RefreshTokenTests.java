package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.RefreshTokenRepository;
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RefreshTokenTests.EndpointConfiguration.class)
class RefreshTokenTests {
    private static final String ISSUER = "https://corretora.test/refresh";
    private static final String AUDIENCE = "corretora-investors";
    private static final String EMAIL = "refresh@example.com";
    private static final String PASSWORD = "Senha Refresh 1!";
    private static final String TEST_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;
    @MockitoSpyBean private RefreshTokenRepository refreshTokenRepository;

    private UUID accountId;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.issuer", () -> ISSUER);
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
        registry.add("app.security.jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.jwt.refresh-token-ttl", () -> "P30D");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void createActiveAccount() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Refresh", "52998224725",
                EMAIL, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void removeAccount() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION_ATTRIBUTES WHERE SESSION_PRIMARY_ID IN "
                + "(SELECT PRIMARY_ID FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?)", accountId.toString());
        jdbcTemplate.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", accountId.toString());
        if (accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId);
        }
    }

    @Test
    void loginStoresOnlyRefreshHashAndRotationIssuesNewCredentials() throws Exception {
        LoginCredentials login = login();
        assertThat(login.refreshCookie()).isNotNull();
        assertThat(login.refreshCookie().getValue()).isNotBlank();
        assertThat(login.refreshCookie().getValue()).doesNotContain("=");
        assertThat(login.loginResponse().getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "Path=/api/auth", "SameSite=Lax");

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE account_id = ?",
                String.class, accountId);
        assertThat(storedHash).isNotEqualTo(login.refreshCookie().getValue());
        assertThat(storedHash).hasSize(43);

        MvcResult rotated = refresh(login.refreshCookie()).andExpect(status().isNoContent()).andReturn();
        Cookie next = rotated.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(next).isNotNull();
        assertThat(next.getValue()).isNotEqualTo(login.refreshCookie().getValue());
        assertThat(rotated.getResponse().getHeader(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");

        refresh(next).andExpect(status().isNoContent());
    }

    @Test
    void reuseOfConsumedRefreshRevokesItsWholeFamily() throws Exception {
        LoginCredentials login = login();
        Cookie next = refresh(login.refreshCookie()).andReturn().getResponse().getCookie("REFRESH_TOKEN");

        refresh(login.refreshCookie()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
        refresh(next).andExpect(status().isUnauthorized());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND revoked_at IS NOT NULL",
                Integer.class, accountId)).isEqualTo(2);
    }

    @Test
    void missingOrInvalidRefreshIsUnauthorized() throws Exception {
        refresh(null).andExpect(status().isUnauthorized());
        refresh(new Cookie("REFRESH_TOKEN", "not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredRefreshIsUnauthorized() throws Exception {
        LoginCredentials login = login();
        jdbcTemplate.update("UPDATE refresh_token SET expires_at = ? WHERE account_id = ?",
                OffsetDateTime.now().minusMinutes(1), accountId);
        entityManager.clear();

        refresh(login.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void rotationFailureRollsBackConsumedTokenAndSuccessor() throws Exception {
        LoginCredentials login = login();
        String originalHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE account_id = ? AND revoked_at IS NULL "
                        + "AND replaced_by_id IS NULL",
                String.class, accountId);
        String originalFamilyId = jdbcTemplate.queryForObject(
                "SELECT CAST(family_id AS VARCHAR) FROM refresh_token WHERE account_id = ? AND token_hash = ?",
                String.class, accountId, originalHash);
        org.mockito.Mockito.doThrow(new IllegalStateException("falha simulada no flush da rotação"))
                .when(refreshTokenRepository).flush();

        refresh(login.refreshCookie()).andExpect(status().isInternalServerError());
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ?",
                Integer.class, accountId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND token_hash = ? "
                        + "AND revoked_at IS NULL AND replaced_by_id IS NULL",
                Integer.class, accountId, originalHash)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT token_hash FROM refresh_token WHERE account_id = ? AND token_hash = ?",
                String.class, accountId, originalHash)).isEqualTo(originalHash);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT CAST(family_id AS VARCHAR) FROM refresh_token WHERE account_id = ? AND token_hash = ?",
                String.class, accountId, originalHash)).isEqualTo(originalFamilyId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND replaced_by_id IS NOT NULL",
                Integer.class, accountId)).isZero();
    }

    @Test
    void logoutRevokesRefreshFamilyAndClearsCookie() throws Exception {
        LoginCredentials login = login();
        CsrfCredentials csrf = csrf();

        mockMvc.perform(post("/api/auth/logout").cookie(login.refreshCookie(), csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("REFRESH_TOKEN=;")));

        refresh(login.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeRevokesRefreshFamilyWithoutChangingFinancialOwnership() throws Exception {
        LoginCredentials login = login();
        CsrfCredentials csrf = csrf();

        mockMvc.perform(patch("/api/accounts/me/password")
                        .header(HttpHeaders.AUTHORIZATION, login.accessHeader())
                        .cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", "Senha Refresh Nova 1!"))))
                .andExpect(status().isNoContent());

        refresh(login.refreshCookie()).andExpect(status().isUnauthorized());
        assertThat(accountRepository.findById(accountId)).isPresent();

        mockMvc.perform(get("/api/t15/refresh-private")
                        .header(HttpHeaders.AUTHORIZATION, login.accessHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void emailChangeRevokesRefreshFamily() throws Exception {
        LoginCredentials login = login();
        CsrfCredentials csrf = csrf();

        mockMvc.perform(patch("/api/accounts/me/email")
                        .header(HttpHeaders.AUTHORIZATION, login.accessHeader())
                        .cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newEmail", "refresh-new@example.com", "currentPassword", PASSWORD))))
                .andExpect(status().isNoContent());

        refresh(login.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void cookieRefreshAndLogoutRequireCsrfToken() throws Exception {
        LoginCredentials login = login();

        mockMvc.perform(post("/api/auth/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout").cookie(login.refreshCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactivationRevokesRefreshFamilyAndAccountRemainsFinanciallyPresent() throws Exception {
        LoginCredentials login = login();
        CsrfCredentials csrf = csrf();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/accounts/me")
                        .header(HttpHeaders.AUTHORIZATION, login.accessHeader())
                        .cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD, "confirmation", "Excluir"))))
                .andExpect(status().isNoContent());

        refresh(login.refreshCookie()).andExpect(status().isUnauthorized());
        assertThat(accountRepository.findById(accountId)).isPresent()
                .get().extracting(Account::getStatus).isEqualTo(com.projeto.gestao.domain.model.AccountStatus.INACTIVE);
    }

    @Test
    void validAccessTokenKeepsInvestorIdentityAfterRefresh() throws Exception {
        LoginCredentials login = login();
        MvcResult rotated = refresh(login.refreshCookie()).andReturn();

        mockMvc.perform(get("/api/t15/refresh-private")
                        .header(HttpHeaders.AUTHORIZATION,
                                rotated.getResponse().getHeader(HttpHeaders.AUTHORIZATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    private LoginCredentials login() throws Exception {
        CsrfCredentials csrf = csrf();
        MvcResult response = mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isNoContent()).andReturn();
        return new LoginCredentials(response,
                response.getResponse().getCookie("REFRESH_TOKEN"),
                response.getResponse().getHeader(HttpHeaders.AUTHORIZATION));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie cookie) throws Exception {
        CsrfCredentials csrf = csrf();
        var request = post("/api/auth/refresh").cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token());
        if (cookie != null) {
            request.cookie(cookie);
        }
        return mockMvc.perform(request);
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private record LoginCredentials(MvcResult loginResponse, Cookie refreshCookie, String accessHeader) { }

    private record CsrfCredentials(Cookie cookie, String token) { }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @TestConfiguration
    static class EndpointConfiguration {
        @Bean
        PrivateEndpoint privateEndpoint() {
            return new PrivateEndpoint();
        }
    }

    @RestController
    static class PrivateEndpoint {
        @GetMapping("/api/t15/refresh-private")
        Map<String, String> privateRoute(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }
    }
}
