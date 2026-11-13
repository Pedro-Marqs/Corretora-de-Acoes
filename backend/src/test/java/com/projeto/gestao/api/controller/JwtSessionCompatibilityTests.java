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
import com.projeto.gestao.security.JwtTokenService;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JwtSessionCompatibilityTests.EndpointConfiguration.class)
@Transactional
class JwtSessionCompatibilityTests {
    private static final String ISSUER = "https://corretora.test/session-compatibility";
    private static final String AUDIENCE = "corretora-investors";
    private static final String EMAIL = "jwt-session@example.com";
    private static final String PASSWORD = "Senha Sessao 1!";
    private static final String TEST_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private SecurityContextRepository securityContextRepository;

    private UUID accountId;
    private UUID secondAccountId;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "true");
        registry.add("app.security.jwt.issuer", () -> ISSUER);
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
        registry.add("app.security.jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void createActiveAccount() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Sessao", "52998224725",
                EMAIL, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
        secondAccountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(secondAccountId, "Investidor Bearer", "11144477735",
                "jwt-bearer@example.com", passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void explicitlyEnabledCompatibilityKeepsLegacySessionWorking() throws Exception {
        assertThat(securityContextRepository).isInstanceOf(HttpSessionSecurityContextRepository.class);
        Cookie session = login();

        assertThat(session).isNotNull();
        mockMvc.perform(get("/api/t13/session-private").cookie(session))
                .andExpect(status().isOk());
    }

    @Test
    void invalidBearerDoesNotFallbackToAValidJdbcSession() throws Exception {
        Cookie session = login();

        mockMvc.perform(get("/api/t13/session-private").cookie(session)
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());

        String validToken = jwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();
        mockMvc.perform(get("/api/t13/session-private").cookie(session)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void validBearerWinsOverLegacySessionWithoutIdentityConfusion() throws Exception {
        Cookie session = login();
        String token = jwtTokenService.issue(new AccountPrincipal(secondAccountId)).tokenValue();

        mockMvc.perform(get("/api/t13/session-private").cookie(session)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(secondAccountId.toString()));
    }

    @Test
    void cookieAuthenticatedMutationStillRequiresCsrfDuringCompatibility() throws Exception {
        Cookie session = login();

        mockMvc.perform(post("/api/t13/session-private").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("operation", "legacy"))))
                .andExpect(status().isForbidden());
    }

    private Cookie login() throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("SESSION");
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private record CsrfCredentials(Cookie cookie, String token) { }

    @TestConfiguration
    static class EndpointConfiguration {
        @Bean
        PrivateEndpoint privateEndpoint() {
            return new PrivateEndpoint();
        }
    }

    @RestController
    static class PrivateEndpoint {
        @GetMapping("/api/t13/session-private")
        Map<String, String> privateRoute(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("status", "ok", "accountId", principal.accountId().toString());
        }

        @PostMapping("/api/t13/session-private")
        Map<String, String> privateMutation() {
            return Map.of("status", "ok");
        }
    }
}
