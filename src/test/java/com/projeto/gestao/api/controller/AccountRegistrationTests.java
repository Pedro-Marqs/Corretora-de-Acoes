package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;

import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountRegistrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @Autowired private PatrimonialPointRepository patrimonialPointRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void createsActiveAccountInitialMovementAndPatrimonialPointAtomically() throws Exception {
        MvcResult result = register(validPayload("529.982.247-25", " INVESTIDOR@Example.COM "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Maria da Silva"))
                .andExpect(jsonPath("$.balance").value(10000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        Account account = accountRepository.findAll().get(0);
        assertThat(account.getName()).isEqualTo("Maria da Silva");
        assertThat(account.getCpf()).isEqualTo("52998224725");
        assertThat(account.getEmail()).isEqualTo("investidor@example.com");
        assertThat(account.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getPasswordHash()).doesNotContain("SenhaForte1!");
        assertThat(passwordEncoder.matches("SenhaForte1!", account.getPasswordHash())).isTrue();

        var movement = movementRepository.findAll().get(0);
        var point = patrimonialPointRepository.findAll().get(0);
        assertThat(movement.getMovementType()).isEqualTo(MovementType.INITIAL_BALANCE);
        assertThat(movement.getTotalAmount()).isEqualByComparingTo("10000.00");
        assertThat(movement.getRemainingBalance()).isEqualByComparingTo("10000.00");
        assertThat(point.getPatrimonyBrl()).isEqualByComparingTo("10000.00");
        assertThat(movement.getOccurredAt()).isEqualTo(account.getCreatedAt());
        assertThat(point.getRecordedAt()).isEqualTo(account.getCreatedAt());
        assertThat(result.getResponse().getContentAsString()).doesNotContain(
                "52998224725", "investidor@example.com", "SenhaForte1!", account.getPasswordHash());
    }

    @Test
    void rejectsMissingInvalidCpfInvalidEmailAndEveryMissingPasswordRule() throws Exception {
        register(Map.of("name", " ", "cpf", "111.111.111-11", "email", "inválido", "password", "abcdefgh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'cpf')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.message =~ /.*maiúscula.*/)]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.message =~ /.*número.*/)]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.message =~ /.*especial.*/)]").exists());
        assertThat(accountRepository.count()).isZero();
        assertThat(movementRepository.count()).isZero();
        assertThat(patrimonialPointRepository.count()).isZero();
    }

    @Test
    void acceptsPasswordWithSpacesAndPasswordLongerThanBcryptInputLimit() throws Exception {
        String passwordWithSpaces = "Senha forte 1!";
        register(payload("52998224725", "spaces@example.com", passwordWithSpaces))
                .andExpect(status().isCreated());
        Account first = accountRepository.findAll().get(0);
        assertThat(passwordEncoder.matches(passwordWithSpaces, first.getPasswordHash())).isTrue();

        String longPassword = "Aa1!" + "x".repeat(80);
        register(payload("11144477735", "long@example.com", longPassword))
                .andExpect(status().isCreated());
        Account second = accountRepository.findAll().stream()
                .filter(account -> account.getEmail().equals("long@example.com"))
                .findFirst().orElseThrow();
        assertThat(second.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(longPassword, second.getPasswordHash())).isTrue();

        String unicodePrefixBeyond72Bytes = "Aa1!" + "á".repeat(40);
        String unicodeFirst = unicodePrefixBeyond72Bytes + "X";
        String unicodeSecond = unicodePrefixBeyond72Bytes + "Y";
        String unicodeFirstHash = passwordEncoder.encode(unicodeFirst);
        assertThat(passwordEncoder.matches(unicodeFirst, unicodeFirstHash)).isTrue();
        assertThat(passwordEncoder.matches(unicodeSecond, unicodeFirstHash)).isFalse();
    }

    @Test
    void rejectsCpfWithLettersExtraSymbolsOrIncompleteMask() throws Exception {
        for (String cpf : java.util.List.of(
                "529a98224725", "529.982.247-25!", "529.982.247-2", "5299822472-5")) {
            register(validPayload(cpf, cpf.replaceAll("\\W", "") + "@example.com"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'cpf')]").exists());
        }
        assertThat(accountRepository.count()).isZero();
    }

    @Test
    void rejectsDuplicateCpfOrEmailOnlyWhenAccountIsActive() throws Exception {
        register(validPayload("52998224725", "first@example.com")).andExpect(status().isCreated());

        register(validPayload("529.982.247-25", "other@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT_ERROR"));
        register(validPayload("11144477735", " FIRST@EXAMPLE.COM "))
                .andExpect(status().isConflict());
        assertThat(accountRepository.count()).isEqualTo(1);
    }

    @Test
    void inactiveAndDeletedAccountsDoNotBlockNewActiveAccount() throws Exception {
        insertNonActive("52998224725", "old@example.com", "INACTIVE");
        insertNonActive("11144477735", "deleted@example.com", "DELETED");

        register(validPayload("52998224725", "new@example.com")).andExpect(status().isCreated());
        register(validPayload("11144477735", "newer@example.com")).andExpect(status().isCreated());
        assertThat(accountRepository.findAll().stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)).hasSize(2);
    }

    @Test
    void publicRegistrationStillRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload("52998224725", "csrf@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
        assertThat(accountRepository.count()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions register(Map<String, String> payload) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(csrf.getResponse().getContentAsString());
        Cookie cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        return mockMvc.perform(post("/api/accounts")
                .cookie(cookie)
                .header("X-XSRF-TOKEN", body.path("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private Map<String, String> validPayload(String cpf, String email) {
        return payload(cpf, email, "SenhaForte1!");
    }

    private Map<String, String> payload(String cpf, String email, String password) {
        return Map.of("name", "  Maria   da Silva  ", "cpf", cpf,
                "email", email, "password", password);
    }

    private void insertNonActive(String cpf, String email, String status) {
        jdbcTemplate.update("""
                INSERT INTO account
                    (id, name, cpf, email, password_hash, balance, status, created_at, inactivated_at)
                VALUES (?, ?, ?, ?, ?, 10000.00, ?, ?, ?)
                """, java.util.UUID.randomUUID(), "Conta anterior", cpf, email, "hash", status,
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00"),
                OffsetDateTime.parse("2026-01-02T10:00:00-03:00"));
    }
}
