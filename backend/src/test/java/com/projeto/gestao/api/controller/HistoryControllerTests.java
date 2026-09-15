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
import com.projeto.gestao.domain.model.AccountBroker;
import com.projeto.gestao.domain.model.Broker;
import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.BrokerRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryControllerTests {
    private static final String PASSWORD = "Senha Forte 1!";
    private static final OffsetDateTime BASE = OffsetDateTime.parse("2026-09-04T10:00:00-03:00");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accounts;
    @Autowired private BrokerRepository brokers;
    @Autowired private AccountBrokerRepository associations;
    @Autowired private MovementRepository movements;
    @Autowired private PatrimonialPointRepository points;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    private Account first;
    private Account second;
    private AccountBroker firstBroker;
    private AccountBroker secondBroker;

    @BeforeEach
    void setUp() {
        cleanup();
        first = account("52998224725", "history-first@example.com");
        second = account("11144477735", "history-second@example.com");
        firstBroker = association(first, "02332886000104", "Primeira");
        secondBroker = association(second, "10270580000110", "Segunda");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        points.deleteAll();
        movements.deleteAll();
        associations.deleteAll();
        brokers.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void paginatesTwentyAndOrdersByInstantThenIdentifierDescending() throws Exception {
        UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higher = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        movements.save(Movement.deposit(lower, first, amount("10"), amount("10010"), BASE));
        movements.save(Movement.deposit(higher, first, amount("11"), amount("10021"), BASE));
        for (int index = 1; index <= 20; index++) {
            movements.save(Movement.deposit(UUID.randomUUID(), first, amount("10"),
                    amount("10000"), BASE.minusMinutes(index)));
        }
        Cookie session = login(first.getEmail());

        mockMvc.perform(get("/api/history").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.content[0].id").value(higher.toString()))
                .andExpect(jsonPath("$.content[1].id").value(lower.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(22))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/history").param("page", "1").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
        mockMvc.perform(get("/api/history").param("page", "8").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(22))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void appliesEveryFilterAloneAndCombinedBeforePagination() throws Exception {
        association(first, "37033895000136", "Venda");
        Movement purchase = Movement.purchase(UUID.randomUUID(), first, "petr4", Market.BR,
                amount("25"), amount("25"), null, 2, amount("50"), Currency.BRL,
                "Primeira", amount("9950"), BASE);
        movements.save(purchase);
        movements.save(Movement.sale(UUID.randomUUID(), first, "AAPL", Market.US,
                amount("10"), amount("50"), amount("5"), 1, amount("50"), Currency.USD,
                "Venda", amount("10000"), amount("5"),
                BASE.plusHours(1)));
        movements.save(Movement.deposit(UUID.randomUUID(), first, amount("20"),
                amount("10020"), BASE.plusHours(2)));
        Cookie session = login(first.getEmail());

        expectOne(session, "type", "PURCHASE", purchase.getId());
        expectOne(session, "ticker", "PeTr4", purchase.getId());
        expectOne(session, "market", "BR", purchase.getId());
        expectOne(session, "brokerId", firstBroker.getId().toString(), purchase.getId());
        expectOne(session, "from", BASE.minusMinutes(1).toString(), "to",
                BASE.plusMinutes(1).toString(), purchase.getId());
        mockMvc.perform(get("/api/history").cookie(session)
                        .param("type", "PURCHASE").param("ticker", "PETR4")
                        .param("market", "BR").param("brokerId", firstBroker.getId().toString())
                        .param("from", BASE.toString()).param("to", BASE.plusSeconds(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(purchase.getId().toString()));
    }

    @Test
    void exposesCompleteTransferProjectionAndMatchesEitherBrokerRole() throws Exception {
        AccountBroker destination = association(first, "34028316000103", "Destino");
        Movement transfer = movements.save(Movement.transfer(UUID.randomUUID(), first, "PETR4",
                Market.BR, 3, amount("75"), Currency.BRL,
                "Primeira", "Destino",
                amount("9925"), BASE));
        Cookie session = login(first.getEmail());

        mockMvc.perform(get("/api/history").cookie(session)
                        .param("brokerId", destination.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(transfer.getId().toString()))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
                .andExpect(jsonPath("$.content[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.content[0].market").value("BR"))
                .andExpect(jsonPath("$.content[0].quantity").value(3))
                .andExpect(jsonPath("$.content[0].totalAmount").value(75.00))
                .andExpect(jsonPath("$.content[0].currency").value("BRL"))
                .andExpect(jsonPath("$.content[0].originBrokerName").value("Primeira"))
                .andExpect(jsonPath("$.content[0].destinationBrokerName").value("Destino"))
                .andExpect(jsonPath("$.content[0].occurredAt").exists())
                .andExpect(jsonPath("$.content[0].remainingBalance").value(9925.00))
                .andExpect(jsonPath("$.content[0].account").doesNotExist());
    }

    @Test
    void isolatesAccountsAndForeignBrokerFilterWithoutChangingPersistedData() throws Exception {
        movements.save(Movement.deposit(UUID.randomUUID(), first, amount("10"), amount("10010"), BASE));
        movements.save(Movement.purchase(UUID.randomUUID(), second, "AAPL", Market.US,
                amount("10"), amount("50"), amount("5"), 1, amount("50"), Currency.USD,
                "Segunda", amount("9950"), BASE));
        Cookie session = login(first.getEmail());
        long movementCount = movements.count();
        long pointCount = points.count();

        for (int repeat = 0; repeat < 2; repeat++) {
            mockMvc.perform(get("/api/history").cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
        mockMvc.perform(get("/api/history").cookie(session)
                        .param("brokerId", secondBroker.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(movements.count()).isEqualTo(movementCount);
        assertThat(points.count()).isEqualTo(pointCount);
    }

    @Test
    void rejectsAuthenticationAndEveryMalformedParameterWithoutMutation() throws Exception {
        movements.save(Movement.deposit(UUID.randomUUID(), first, amount("10"), amount("10010"), BASE));
        mockMvc.perform(get("/api/history")).andExpect(status().isUnauthorized());
        Cookie session = login(first.getEmail());

        for (String[] parameter : new String[][] {
                {"page", "-1"}, {"page", "abc"}, {"from", "yesterday"},
                {"type", "UNKNOWN"}, {"ticker", "PETR 4"}, {"brokerId", "invalid"},
                {"market", "EU"}}) {
            mockMvc.perform(get("/api/history").cookie(session)
                            .param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(get("/api/history").cookie(session)
                        .param("from", BASE.plusDays(1).toString()).param("to", BASE.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(movements.count()).isEqualTo(1);
        assertThat(points.count()).isZero();
    }

    private void expectOne(Cookie session, String name, String value, UUID id) throws Exception {
        mockMvc.perform(get("/api/history").cookie(session).param(name, value))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    private void expectOne(Cookie session, String firstName, String firstValue,
            String secondName, String secondValue, UUID id) throws Exception {
        mockMvc.perform(get("/api/history").cookie(session).param(firstName, firstValue)
                        .param(secondName, secondValue))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    private Account account(String cpf, String email) {
        return accounts.save(Account.create(UUID.randomUUID(), "Investor", cpf, email,
                passwordEncoder.encode(PASSWORD), amount("10000"), BASE));
    }

    private AccountBroker association(Account owner, String cnpj, String name) {
        Broker broker = brokers.save(Broker.create(UUID.randomUUID(), cnpj, name + " SA", name,
                "ATIVA", "CTVM", "01001000", "Rua A", "1", null, "Centro",
                "São Paulo", "SP", BASE));
        return associations.save(AccountBroker.create(UUID.randomUUID(), owner, broker, BASE));
    }

    private Cookie login(String email) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    private record CsrfCredentials(Cookie cookie, String token) { }
}
