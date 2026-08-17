package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.projeto.gestao.repository.SessionRevocationRepository;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountManagementRollbackTests {
    private static final String EMAIL = "management-rollback@example.com";
    private static final String PASSWORD = "Senha Atual 1!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    @MockitoSpyBean private SessionRevocationRepository sessionRevocationRepository;

    private UUID accountId;

    @AfterEach
    void cleanup() {
        if (accountId != null) {
            accountRepository.deleteById(accountId);
        }
    }

    @Test
    void rollsBackCredentialWhenSessionRevocationFails() throws Exception {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Rollback", "52998224725", EMAIL,
                passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
        Cookie firstSession = login();
        Cookie secondSession = login();
        assertThat(sessionRepository.findByPrincipalName(accountId.toString())).hasSize(2);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("falha simulada após revogação");
        }).when(sessionRevocationRepository).revokeAll(any(UUID.class));
        CsrfCredentials csrf = csrf();

        mockMvc.perform(patch("/api/accounts/me/email").cookie(firstSession, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "newEmail", "should-rollback@example.com",
                                "currentPassword", PASSWORD))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        assertThat(accountRepository.findById(accountId).orElseThrow().getEmail()).isEqualTo(EMAIL);
        assertThat(sessionRepository.findByPrincipalName(accountId.toString())).hasSize(2);
        mockMvc.perform(get("/api/accounts/me").cookie(firstSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/accounts/me").cookie(secondSession))
                .andExpect(status().isOk());
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
}
