package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_POSTGRES_TESTS", matches = "(?i)true")
class LocalPostgreSqlAccountRegistrationTests {
    private static final String DATABASE_NAME = "gestao_acoes_test";

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
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    void persistsAccountInitialMovementAndPatrimonialPointInPostgreSql() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrf.getResponse().getContentAsString());
        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfBody.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Investidor PostgreSQL",
                                "cpf", "52998224725",
                                "email", "t07-postgres@example.com",
                                "password", "Senha PostgreSQL 1!"))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID accountId = UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).path("accountId").asText());
        entityManager.flush();
        assertThat(count("account", "id", accountId)).isEqualTo(1);
        assertThat(count("movement", "account_id", accountId)).isEqualTo(1);
        assertThat(count("patrimonial_point", "account_id", accountId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT movement_type FROM movement WHERE account_id = ?", String.class, accountId))
                .isEqualTo("INITIAL_BALANCE");
    }

    @Test
    void postgreSqlEnforcesActiveCpfUniqueness() {
        UUID firstId = UUID.randomUUID();
        insertActiveAccount(firstId, "11144477735", "constraint-first@example.com");

        assertThatThrownBy(() -> insertActiveAccount(
                UUID.randomUUID(), "11144477735", "constraint-second@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Integer count(String table, String column, UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, id);
    }

    private void insertActiveAccount(UUID id, String cpf, String email) {
        jdbcTemplate.update("""
                INSERT INTO account
                    (id, name, cpf, email, password_hash, balance, status, created_at)
                VALUES (?, ?, ?, ?, ?, 10000.00, 'ACTIVE', ?)
                """, id, "Constraint PostgreSQL", cpf, email, "hash-for-constraint-test",
                Timestamp.from(Instant.now()));
    }
}
