package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(LocalPostgreSqlAuthenticationTests.EndpointConfiguration.class)
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_POSTGRES_TESTS", matches = "(?i)true")
class LocalPostgreSqlAuthenticationTests {
    private static final String DATABASE_NAME = "gestao_acoes_test";
    private static final String EMAIL = "t08-postgres@example.com";
    private static final String PASSWORD = "Senha PostgreSQL 1!";

    @DynamicPropertySource
    static void localPostgres(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault(
                "LOCAL_POSTGRES_TEST_URL", "jdbc:postgresql://localhost:5432/" + DATABASE_NAME);
        if (!url.matches("jdbc:postgresql://[^/]+/" + DATABASE_NAME + "(?:\\?.*)?")) {
            throw new IllegalStateException("O teste aceita exclusivamente o banco " + DATABASE_NAME);
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> requiredEnvironment("LOCAL_POSTGRES_TEST_USER"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("LOCAL_POSTGRES_TEST_PASSWORD"));
        registry.add("spring.flyway.locations", () ->
                "classpath:db/migration/common,classpath:db/migration/postgresql");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável obrigatória ausente: " + name);
        }
        return value;
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID accountId;

    @BeforeEach
    void createAccount() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor T08 PostgreSQL",
                "52998224725", EMAIL, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void cleanupAccount() {
        if (accountId != null && accountRepository.existsById(accountId)) {
            jdbcTemplate.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", accountId.toString());
            accountRepository.deleteById(accountId);
        }
    }

    @Test
    void persistsAndRecoversMinimalPrincipalInExclusivePostgreSqlTestDatabase() throws Exception {
        CsrfCredentials csrf = csrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isNoContent()).andReturn();
        Cookie session = login.getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();

        byte[] serializedContext = jdbcTemplate.queryForObject("""
                SELECT A.ATTRIBUTE_BYTES
                  FROM SPRING_SESSION_ATTRIBUTES A
                  JOIN SPRING_SESSION S ON S.PRIMARY_ID = A.SESSION_PRIMARY_ID
                 WHERE A.ATTRIBUTE_NAME = 'SPRING_SECURITY_CONTEXT'
                   AND S.PRINCIPAL_NAME = ?
                """, byte[].class, accountId.toString());
        assertThat(new String(serializedContext, StandardCharsets.ISO_8859_1))
                .doesNotContain(EMAIL, "Investidor T08 PostgreSQL", "52998224725");

        mockMvc.perform(get("/api/t08/postgres/private").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void changesCredentialAndRevokesEverySessionInExclusivePostgreSqlTestDatabase() throws Exception {
        Cookie first = login();
        Cookie second = login();
        CsrfCredentials csrf = csrf();

        mockMvc.perform(patch("/api/accounts/me/email").cookie(first, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newEmail", "t09-postgres-new@example.com",
                                "currentPassword", PASSWORD))))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT email FROM account WHERE id = ?", String.class, accountId))
                .isEqualTo("t09-postgres-new@example.com");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?",
                Integer.class, accountId.toString())).isZero();
        mockMvc.perform(get("/api/t08/postgres/private").cookie(second))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void inactivatesAndReactivatesPreservedAccountInExclusivePostgreSqlTestDatabase() throws Exception {
        Cookie first = login();
        Cookie second = login();
        CsrfCredentials csrf = csrf();

        mockMvc.perform(delete("/api/accounts/me").cookie(first, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD, "confirmation", "Excluir"))))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM account WHERE id=?", String.class, accountId))
                .isEqualTo("INACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT balance FROM account WHERE id=?", BigDecimal.class, accountId))
                .isEqualByComparingTo("10000.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME=?", Integer.class, accountId.toString()))
                .isZero();
        mockMvc.perform(get("/api/t08/postgres/private").cookie(second)).andExpect(status().isUnauthorized());

        csrf = csrf();
        mockMvc.perform(post("/api/accounts/reactivation").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("cpf", "52998224725"))))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM account WHERE id=?", String.class, accountId))
                .isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT inactivated_at FROM account WHERE id=?", Object.class, accountId))
                .isNull();
    }

    private Cookie login() throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private record CsrfCredentials(Cookie cookie, String token) { }

    @TestConfiguration
    static class EndpointConfiguration {
        @Bean
        PostgreSqlPrivateEndpoint postgreSqlPrivateEndpoint() {
            return new PostgreSqlPrivateEndpoint();
        }
    }

    @RestController
    static class PostgreSqlPrivateEndpoint {
        @GetMapping("/api/t08/postgres/private")
        Map<String, String> privateRoute(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }
    }
}
