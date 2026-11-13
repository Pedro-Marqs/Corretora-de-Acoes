package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RefreshTokenReuseTransactionRegressionTests.EndpointConfiguration.class)
class RefreshTokenReuseTransactionRegressionTests {
    private static final String ISSUER = "https://corretora.test/refresh-regression";
    private static final String AUDIENCE = "corretora-investors";
    private static final String EMAIL = "refresh-regression@example.com";
    private static final String PASSWORD = "Senha Refresh Regression 1!";
    private static final String SECOND_EMAIL = "refresh-regression-second@example.com";
    private static final String SECOND_PASSWORD = "Senha Refresh Regression 2!";
    private static final String TEST_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID accountId;
    private UUID secondAccountId;

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
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Refresh Regression",
                "52998224725", EMAIL, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
        secondAccountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(secondAccountId, "Segundo Investidor Refresh Regression",
                "11144477735", SECOND_EMAIL, passwordEncoder.encode(SECOND_PASSWORD),
                new BigDecimal("10000.00"), OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void removeAccount() {
        if (accountId != null && accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId);
        }
        if (secondAccountId != null && accountRepository.existsById(secondAccountId)) {
            accountRepository.deleteById(secondAccountId);
        }
    }

    @Test
    void reuseCommitsFamilyRevocationBeforeReturningUnauthorized() throws Exception {
        Cookie tokenA = login().refreshCookie();
        Cookie tokenB = refresh(tokenA).andExpect(status().isNoContent()).andReturn()
                .getResponse().getCookie("REFRESH_TOKEN");

        refresh(tokenA).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));

        refresh(tokenB).andExpect(status().isUnauthorized());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE account_id = ? AND revoked_at IS NOT NULL",
                Integer.class, accountId)).isEqualTo(2);
    }

    @Test
    void repeatedReuseOfRevokedFamilyRemainsUnauthorized() throws Exception {
        Cookie tokenA = login().refreshCookie();
        Cookie tokenB = refresh(tokenA).andExpect(status().isNoContent()).andReturn()
                .getResponse().getCookie("REFRESH_TOKEN");

        refresh(tokenA).andExpect(status().isUnauthorized());
        refresh(tokenA).andExpect(status().isUnauthorized());
        refresh(tokenB).andExpect(status().isUnauthorized());
    }

    @Test
    void revocationDoesNotCrossFamiliesOrAccounts() throws Exception {
        Cookie firstFamilyTokenA = login().refreshCookie();
        Cookie secondFamilyTokenA = login().refreshCookie();
        Cookie otherAccountTokenA = login(SECOND_EMAIL, SECOND_PASSWORD).refreshCookie();

        Cookie firstFamilyTokenB = refresh(firstFamilyTokenA).andExpect(status().isNoContent()).andReturn()
                .getResponse().getCookie("REFRESH_TOKEN");
        Cookie secondFamilyTokenB = refresh(secondFamilyTokenA).andExpect(status().isNoContent()).andReturn()
                .getResponse().getCookie("REFRESH_TOKEN");

        refresh(firstFamilyTokenA).andExpect(status().isUnauthorized());
        refresh(firstFamilyTokenB).andExpect(status().isUnauthorized());
        refresh(secondFamilyTokenB).andExpect(status().isNoContent());
        refresh(otherAccountTokenA).andExpect(status().isNoContent());
    }

    private LoginCredentials login() throws Exception {
        return login(EMAIL, PASSWORD);
    }

    private LoginCredentials login(String email, String password) throws Exception {
        CsrfCredentials csrf = csrf();
        MvcResult response = mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isNoContent()).andReturn();
        return new LoginCredentials(response.getResponse().getCookie("REFRESH_TOKEN"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie cookie) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/refresh").cookie(cookie, csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()));
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"), body.path("token").asText());
    }

    private record LoginCredentials(Cookie refreshCookie) { }

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
        @GetMapping("/api/t15/refresh-regression-private")
        Map<String, String> privateRoute(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }
    }
}
