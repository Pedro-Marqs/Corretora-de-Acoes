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
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
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
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

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
        accountRepository.deleteAll();
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
