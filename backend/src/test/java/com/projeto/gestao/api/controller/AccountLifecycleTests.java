package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.repository.AccountRepository;
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
class AccountLifecycleTests {
    private static final String CPF = "52998224725";
    private static final String EMAIL = "lifecycle@example.com";
    private static final String PASSWORD = "Senha Atual 1!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    private UUID accountId;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM account_broker");
        jdbcTemplate.update("DELETE FROM patrimonial_point");
        jdbcTemplate.update("DELETE FROM movement");
        jdbcTemplate.update("DELETE FROM asset");
        jdbcTemplate.update("DELETE FROM broker");
        jdbcTemplate.update("DELETE FROM account");
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Conta Preservada", CPF, EMAIL,
                passwordEncoder.encode(PASSWORD), new BigDecimal("12345.67"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM account_broker");
        jdbcTemplate.update("DELETE FROM patrimonial_point");
        jdbcTemplate.update("DELETE FROM movement");
        jdbcTemplate.update("DELETE FROM asset");
        jdbcTemplate.update("DELETE FROM broker");
        jdbcTemplate.update("DELETE FROM account");
    }

    @Test
    void inactivatesLogicallyRevokesEverySessionAndPreservesData() throws Exception {
        insertPreservedPortfolioData();
        Cookie first = login();
        Cookie second = login();

        request(delete("/api/accounts/me"), first,
                Map.of("email", " LIFECYCLE@example.com ", "password", PASSWORD, "confirmation", "Excluir"))
                .andExpect(status().isNoContent());

        Account inactive = accountRepository.findById(accountId).orElseThrow();
        assertThat(inactive.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(inactive.getInactivatedAt()).isNotNull();
        assertThat(inactive.getBalance()).isEqualByComparingTo("12345.67");
        assertThat(inactive.getName()).isEqualTo("Conta Preservada");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_broker WHERE account_id=?", Integer.class, accountId)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM position WHERE account_id=?", Integer.class, accountId)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM movement WHERE account_id=?", Integer.class, accountId)).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM patrimonial_point WHERE account_id=?", Integer.class, accountId)).isOne();
        mockMvc.perform(get("/api/accounts/me").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/me").cookie(second)).andExpect(status().isUnauthorized());
        loginRequest().andExpect(status().isUnauthorized());
    }

    @Test
    void invalidConfirmationOrCredentialsDoNotChangeAccountAndCsrfIsRequired() throws Exception {
        Cookie session = login();
        request(delete("/api/accounts/me"), session,
                Map.of("email", EMAIL, "password", PASSWORD, "confirmation", "excluir"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.message == 'Confirmação deve ser exatamente Excluir.')]").exists());
        request(delete("/api/accounts/me"), session,
                Map.of("email", EMAIL, "password", "Errada 1!", "confirmation", "Excluir"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
        mockMvc.perform(delete("/api/accounts/me").cookie(session).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", EMAIL, "password", PASSWORD, "confirmation", "Excluir"))))
                .andExpect(status().isForbidden());
        assertThat(accountRepository.findById(accountId).orElseThrow().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void validationMessagesAreReadableAndDeleteRequestNeverPrintsSecrets() throws Exception {
        Cookie session = login();
        request(delete("/api/accounts/me"), session,
                Map.of("email", "invalido", "password", PASSWORD, "confirmation", "Excluir"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.message == 'E-mail inválido.')]").exists());
        publicPost("/api/accounts/reactivation/check", Map.of("cpf", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.message == 'CPF é obrigatório.')]").exists());

        String representation = new DeleteAccountRequest(
                "secret@example.com", "Senha Secreta 1!", "Excluir").toString();
        assertThat(representation).isEqualTo("DeleteAccountRequest[redacted]")
                .doesNotContain("secret@example.com", "Senha Secreta 1!", "Excluir");
    }

    @Test
    void checksAndReactivatesSingleInactiveAccountWithoutCreatingSession() throws Exception {
        jdbcTemplate.update("UPDATE account SET status='INACTIVE', inactivated_at=? WHERE id=?",
                OffsetDateTime.parse("2026-02-01T10:00:00-03:00"), accountId);
        MvcResult check = publicPost("/api/accounts/reactivation/check", Map.of("cpf", "529.982.247-25"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reactivationAvailable").value(true))
                .andExpect(jsonPath("$.email").doesNotExist()).andReturn();
        assertThat(check.getResponse().getCookie("SESSION")).isNull();
        MvcResult result = publicPost("/api/accounts/reactivation", Map.of("cpf", CPF))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(result.getResponse().getCookie("SESSION")).isNull();
        Account active = accountRepository.findById(accountId).orElseThrow();
        assertThat(active.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(active.getInactivatedAt()).isNull();
        assertThat(active.getBalance()).isEqualByComparingTo("12345.67");
        assertThat(login()).isNotNull();
    }

    @Test
    void rejectsActiveDeletedMultipleInactiveAndActiveEmailConflict() throws Exception {
        publicPost("/api/accounts/reactivation/check", Map.of("cpf", CPF)).andExpect(status().isConflict());
        jdbcTemplate.update("UPDATE account SET status='DELETED' WHERE id=?", accountId);
        publicPost("/api/accounts/reactivation", Map.of("cpf", CPF)).andExpect(status().isConflict());

        jdbcTemplate.update("UPDATE account SET status='INACTIVE', inactivated_at=CURRENT_TIMESTAMP WHERE id=?", accountId);
        insert(AccountStatus.INACTIVE, CPF, "second@example.com");
        publicPost("/api/accounts/reactivation", Map.of("cpf", CPF)).andExpect(status().isConflict());

        jdbcTemplate.update("DELETE FROM account WHERE email='second@example.com'");
        insert(AccountStatus.ACTIVE, "11144477735", EMAIL);
        publicPost("/api/accounts/reactivation", Map.of("cpf", CPF)).andExpect(status().isConflict());
        assertThat(accountRepository.findById(accountId).orElseThrow().getStatus()).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void newRegistrationCanReuseInactiveCpfAndEmail() throws Exception {
        jdbcTemplate.update("UPDATE account SET status='INACTIVE', inactivated_at=CURRENT_TIMESTAMP WHERE id=?", accountId);
        publicPost("/api/accounts", Map.of("name", "Nova Conta", "cpf", CPF, "email", EMAIL,
                "password", "Nova Senha 2!"))
                .andExpect(status().isCreated());
        assertThat(accountRepository.findAll()).hasSize(2);
        assertThat(accountRepository.findAllByCpfAndStatus(CPF, AccountStatus.INACTIVE)).hasSize(1);
        assertThat(accountRepository.findAllByCpfAndStatus(CPF, AccountStatus.ACTIVE)).hasSize(1);
    }

    private void insert(AccountStatus status, String cpf, String email) {
        Account account = Account.create(UUID.randomUUID(), "Outra", cpf, email,
                passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"), OffsetDateTime.now());
        accountRepository.saveAndFlush(account);
        if (status == AccountStatus.INACTIVE) {
            jdbcTemplate.update("UPDATE account SET status='INACTIVE', inactivated_at=CURRENT_TIMESTAMP WHERE id=?", account.getId());
        }
    }

    private void insertPreservedPortfolioData() {
        UUID brokerId = UUID.randomUUID();
        UUID accountBrokerId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO broker (id,cnpj,corporate_name,trade_name,registration_status,cvm_category,
                  postal_code,street,number,district,city,state,updated_at)
                VALUES (?,?,?,?,?,'CTVM','01001000','Rua Teste','1','Centro','Sao Paulo','SP',CURRENT_TIMESTAMP)
                """, brokerId, "12345678000195", "Corretora Teste", "Corretora", "ATIVA");
        jdbcTemplate.update("""
                INSERT INTO account_broker (id,account_id,broker_id,status,associated_at)
                VALUES (?,?,?,'ACTIVE',CURRENT_TIMESTAMP)
                """, accountBrokerId, accountId, brokerId);
        jdbcTemplate.update("INSERT INTO asset (id,ticker,name,market,currency) VALUES (?,?,?,'BR','BRL')",
                assetId, "TEST3", "Ativo Teste");
        jdbcTemplate.update("""
                INSERT INTO position (id,account_id,account_broker_id,asset_id,quantity,average_price,total_cost)
                VALUES (?,?,?,?,10,20.00,200.00)
                """, UUID.randomUUID(), accountId, accountBrokerId, assetId);
        jdbcTemplate.update("""
                INSERT INTO movement (id,account_id,movement_type,total_amount,currency,occurred_at,remaining_balance)
                VALUES (?,?,'INITIAL_BALANCE',12345.67,'BRL',CURRENT_TIMESTAMP,12345.67)
                """, movementId, accountId);
        jdbcTemplate.update("""
                INSERT INTO patrimonial_point
                    (id,account_id,movement_id,recorded_at,balance_brl,positions_value_brl,patrimony_brl)
                VALUES (?,?,?,CURRENT_TIMESTAMP,12145.67,200.00,12345.67)
                """, UUID.randomUUID(), accountId, movementId);
    }

    private Cookie login() throws Exception {
        return loginRequest().andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");
    }

    private org.springframework.test.web.servlet.ResultActions loginRequest() throws Exception {
        return publicPost("/api/auth/login", Map.of("email", EMAIL, "password", PASSWORD));
    }

    private org.springframework.test.web.servlet.ResultActions publicPost(String path, Map<String, String> body) throws Exception {
        Csrf csrf = csrf();
        return mockMvc.perform(post(path).cookie(csrf.cookie).header("X-XSRF-TOKEN", csrf.token)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private org.springframework.test.web.servlet.ResultActions request(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            Cookie session, Map<String, String> body) throws Exception {
        Csrf csrf = csrf();
        return mockMvc.perform(builder.cookie(session, csrf.cookie).header("X-XSRF-TOKEN", csrf.token)
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(result.getResponse().getCookie("XSRF-TOKEN"), node.path("token").asText());
    }

    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
    private record Csrf(Cookie cookie, String token) { }
}
