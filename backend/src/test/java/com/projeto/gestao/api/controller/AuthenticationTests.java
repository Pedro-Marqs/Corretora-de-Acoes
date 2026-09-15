package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.Cookie;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthenticationTests.EndpointConfiguration.class)
@Transactional
class AuthenticationTests {
    private static final String EMAIL = "login@example.com";
    private static final String PASSWORD = "Senha Login 1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID accountId;

    @BeforeEach
    void createActiveAccount() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Login", "52998224725",
                EMAIL, passwordEncoder.encode(PASSWORD), new java.math.BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @Test
    void validLoginCreatesJdbcSessionThatAccessesPrivateRoute() throws Exception {
        Cookie session = login(EMAIL.toUpperCase(), PASSWORD).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("SESSION");

        assertThat(session).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID IS NOT NULL", Integer.class))
                .isPositive();
        mockMvc.perform(get("/api/t08/private").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void loginChangesPreexistingSessionIdToPreventFixation() throws Exception {
        org.springframework.mock.web.MockHttpSession preLoginSession =
                new org.springframework.mock.web.MockHttpSession();
        String oldId = preLoginSession.getId();

        Cookie authenticatedSession = login(EMAIL, PASSWORD, preLoginSession)
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");

        assertThat(authenticatedSession).isNotNull();
        String decodedAuthenticatedId = new String(
                Base64.getDecoder().decode(authenticatedSession.getValue()), StandardCharsets.UTF_8);
        assertThat(decodedAuthenticatedId).isNotEqualTo(oldId);
    }

    @Test
    void wrongPasswordUnknownEmailAndInactiveAccountReturnSameNeutralError() throws Exception {
        jdbcTemplate.update("UPDATE account SET status = 'INACTIVE', inactivated_at = ? WHERE id = ?",
                OffsetDateTime.parse("2026-01-02T10:00:00-03:00"), accountId);
        MvcResult inactive = login(EMAIL, PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"))
                .andReturn();
        jdbcTemplate.update("UPDATE account SET status = 'ACTIVE', inactivated_at = NULL WHERE id = ?", accountId);

        MvcResult wrong = login(EMAIL, "Senha Errada 1!").andExpect(status().isUnauthorized()).andReturn();
        MvcResult unknown = login("unknown@example.com", PASSWORD)
                .andExpect(status().isUnauthorized()).andReturn();

        assertThat(inactive.getResponse().getCookie("SESSION")).isNull();
        assertThat(wrong.getResponse().getCookie("SESSION")).isNull();
        assertThat(unknown.getResponse().getCookie("SESSION")).isNull();
        String inactiveBody = inactive.getResponse().getContentAsString();
        String wrongBody = wrong.getResponse().getContentAsString();
        String unknownBody = unknown.getResponse().getContentAsString();

        assertThat(message(inactiveBody)).isEqualTo(message(wrongBody)).isEqualTo(message(unknownBody));
        assertThat(inactiveBody + wrongBody + unknownBody)
                .doesNotContain(EMAIL, PASSWORD, "inactive", "INACTIVE");
    }

    @Test
    void structurallyInvalidLoginReturnsValidationError() throws Exception {
        login("invalid", " ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());

        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginAndLogoutRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials(EMAIL, PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));

        Cookie session = login(EMAIL, PASSWORD).andReturn().getResponse().getCookie("SESSION");
        mockMvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @Test
    void logoutInvalidatesOnlyCurrentSessionAndExpiresCookie() throws Exception {
        Cookie first = login(EMAIL, PASSWORD).andReturn().getResponse().getCookie("SESSION");
        Cookie second = login(EMAIL, PASSWORD).andReturn().getResponse().getCookie("SESSION");
        CsrfCredentials csrf = csrf();

        mockMvc.perform(post("/api/auth/logout").cookie(first, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("SESSION=;")))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(get("/api/t08/private").cookie(first)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/t08/private").cookie(second)).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return login(email, password, null);
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String email, String password, org.springframework.mock.web.MockHttpSession session) throws Exception {
        CsrfCredentials csrf = csrf();
        var request = post("/api/auth/login").cookie(csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials(email, password)));
        if (session != null) {
            request.session(session);
        }
        return mockMvc.perform(request);
    }

    private Map<String, String> credentials(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText());
    }

    private String message(String body) throws Exception {
        return objectMapper.readTree(body).path("message").asText();
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
        @GetMapping("/api/t08/private")
        Map<String, String> privateRoute(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }
    }
}
