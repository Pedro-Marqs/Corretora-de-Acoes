package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountManagementTests {
    private static final String EMAIL = "maria.silva@example.com";
    private static final String PASSWORD = "Senha Atual 1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    private UUID accountId;

    @BeforeEach
    void createAccount() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM account");
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Maria da Silva", "52998224725",
                EMAIL, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM account");
    }

    @Test
    void returnsOnlyOwnMaskedAccountData() throws Exception {
        Cookie session = login(EMAIL, PASSWORD);

        mockMvc.perform(get("/api/accounts/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.cpf").value("529.***.***-25"))
                .andExpect(jsonPath("$.email").value("m***@example.com"))
                .andExpect(jsonPath("$.accountId").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void missingOrInactivePrincipalAccountReturnsNeutralAuthenticationError() throws Exception {
        Cookie session = login(EMAIL, PASSWORD);
        jdbcTemplate.update("UPDATE account SET status='INACTIVE', inactivated_at=? WHERE id=?",
                OffsetDateTime.parse("2026-01-02T10:00:00-03:00"), accountId);

        mockMvc.perform(get("/api/accounts/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
    }

    @Test
    void changesEmailAndRevokesAllSessionsIncludingCurrent() throws Exception {
        Cookie first = login(EMAIL, PASSWORD);
        Cookie second = login(EMAIL, PASSWORD);

        patchWithCsrf("/api/accounts/me/email", first,
                Map.of("newEmail", " NEW@Example.COM ", "currentPassword", PASSWORD))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("SESSION=;")));

        Account changed = accountRepository.findById(accountId).orElseThrow();
        assertThat(changed.getEmail()).isEqualTo("new@example.com");
        assertThat(sessionRepository.findByPrincipalName(accountId.toString())).isEmpty();
        mockMvc.perform(get("/api/accounts/me").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/me").cookie(second)).andExpect(status().isUnauthorized());
        assertThat(login("new@example.com", PASSWORD)).isNotNull();
    }

    @Test
    void changesPasswordAndOldPasswordStopsWorkingAfterAllSessionsAreRevoked() throws Exception {
        Cookie first = login(EMAIL, PASSWORD);
        Cookie second = login(EMAIL, PASSWORD);
        String newPassword = "Nova Senha 2!";

        patchWithCsrf("/api/accounts/me/password", first,
                Map.of("currentPassword", PASSWORD, "newPassword", newPassword))
                .andExpect(status().isNoContent());

        assertThat(sessionRepository.findByPrincipalName(accountId.toString())).isEmpty();
        loginRequest(EMAIL, PASSWORD).andExpect(status().isUnauthorized());
        assertThat(login(EMAIL, newPassword)).isNotNull();
        mockMvc.perform(get("/api/accounts/me").cookie(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongCurrentPasswordDuplicateActiveEmailAndInvalidInputsWithoutChanges() throws Exception {
        Cookie session = login(EMAIL, PASSWORD);
        accountRepository.saveAndFlush(Account.create(UUID.randomUUID(), "Outra", "11144477735",
                "used@example.com", passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));

        patchWithCsrf("/api/accounts/me/email", session,
                Map.of("newEmail", "other@example.com", "currentPassword", "Errada 1!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
        patchWithCsrf("/api/accounts/me/email", session,
                Map.of("newEmail", "USED@example.com", "currentPassword", PASSWORD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT_ERROR"));
        patchWithCsrf("/api/accounts/me/password", session,
                Map.of("currentPassword", PASSWORD, "newPassword", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Account unchanged = accountRepository.findById(accountId).orElseThrow();
        assertThat(unchanged.getEmail()).isEqualTo(EMAIL);
        assertThat(passwordEncoder.matches(PASSWORD, unchanged.getPasswordHash())).isTrue();
        assertThat(sessionRepository.findByPrincipalName(accountId.toString())).isNotEmpty();
    }

    @Test
    void inactiveAccountEmailDoesNotBlockChangeAndPatchRequiresCsrf() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO account (id,name,cpf,email,password_hash,balance,status,created_at,inactivated_at)
                VALUES (?,?,?,?,?,10000.00,'INACTIVE',?,?)
                """, UUID.randomUUID(), "Antiga", "11144477735", "available@example.com", "hash",
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00"),
                OffsetDateTime.parse("2026-01-02T10:00:00-03:00"));
        Cookie session = login(EMAIL, PASSWORD);

        mockMvc.perform(patch("/api/accounts/me/email").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newEmail", "available@example.com", "currentPassword", PASSWORD))))
                .andExpect(status().isForbidden());
        patchWithCsrf("/api/accounts/me/email", session,
                Map.of("newEmail", "available@example.com", "currentPassword", PASSWORD))
                .andExpect(status().isNoContent());
    }

    private Cookie login(String email, String password) throws Exception {
        return loginRequest(email, password).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("SESSION");
    }

    private org.springframework.test.web.servlet.ResultActions loginRequest(
            String email, String password) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private org.springframework.test.web.servlet.ResultActions patchWithCsrf(
            String path, Cookie session, Map<String, String> body) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(patch(path).cookie(session, csrf.cookie())
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
