package com.projeto.gestao.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;

import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.MovementRepository;
import com.projeto.gestao.repository.PatrimonialPointRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountRegistrationRollbackTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private MovementRepository movementRepository;
    @MockitoSpyBean private PatrimonialPointRepository patrimonialPointRepository;

    @Test
    void rollsBackAccountAndMovementWhenPatrimonialPointFails() throws Exception {
        doThrow(new IllegalStateException("falha simulada"))
                .when(patrimonialPointRepository).save(any());
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode csrfBody = objectMapper.readTree(csrf.getResponse().getContentAsString());
        Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/accounts")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfBody.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Conta Rollback", "cpf", "52998224725",
                                "email", "rollback@example.com", "password", "SenhaForte1!"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        org.assertj.core.api.Assertions.assertThat(accountRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(movementRepository.count()).isZero();
        org.assertj.core.api.Assertions.assertThat(patrimonialPointRepository.count()).isZero();
    }
}
