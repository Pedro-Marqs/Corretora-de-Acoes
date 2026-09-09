package com.projeto.gestao.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.projeto.gestao.api.exception.BrokerRuleException;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.service.BrokerAssociationView;
import com.projeto.gestao.service.BrokerLookup;
import com.projeto.gestao.service.BrokerManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrokerControllerTests {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ASSOCIATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String CNPJ = "02332886000104";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BrokerManagementService service;

    @Test
    void searchesByCnpjWithoutCreatingAssociation() throws Exception {
        when(service.lookup(CNPJ)).thenReturn(lookup());

        mockMvc.perform(get("/api/brokers/search").param("cnpj", CNPJ)
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value(CNPJ))
                .andExpect(jsonPath("$.cvmCategory").value("CTVM"))
                .andExpect(jsonPath("$.associationId").doesNotExist());
        verify(service).lookup(CNPJ);
    }

    @Test
    void confirmsUsingOnlySessionAccountAndCnpj() throws Exception {
        when(service.associate(ACCOUNT_ID, CNPJ)).thenReturn(association());

        mockMvc.perform(post("/api/brokers").with(authenticated()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"02332886000104\",\"accountId\":\"00000000-0000-0000-0000-000000000099\",\"corporateName\":\"adulterada\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.associationId").value(ASSOCIATION_ID.toString()))
                .andExpect(jsonPath("$.corporateName").value("XP INVESTIMENTOS S/A"));
        verify(service).associate(ACCOUNT_ID, CNPJ);
    }

    @Test
    void listsAndRemovesOnlyThroughAuthenticatedAccount() throws Exception {
        when(service.listActive(ACCOUNT_ID)).thenReturn(List.of(association()));
        mockMvc.perform(get("/api/brokers").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].associationId").value(ASSOCIATION_ID.toString()));

        mockMvc.perform(delete("/api/brokers/{id}", ASSOCIATION_ID)
                        .with(authenticated()).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).listActive(ACCOUNT_ID);
        verify(service).remove(ACCOUNT_ID, ASSOCIATION_ID);
    }

    @Test
    void validatesInputAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(get("/api/brokers/search").param("cnpj", "123")
                        .with(authenticated()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/brokers")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/brokers").with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\":\"02332886000104\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void mapsFunctionalAndExternalFailuresCentrally() throws Exception {
        when(service.lookup(CNPJ)).thenThrow(BrokerRuleException.notAuthorized())
                .thenThrow(new ExternalDependencyException());
        var request = get("/api/brokers/search").param("cnpj", CNPJ)
                .with(authenticated());
        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_ERROR"));
        mockMvc.perform(get("/api/brokers/search").param("cnpj", CNPJ)
                        .with(authenticated()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_DEPENDENCY_ERROR"));
    }

    private static BrokerLookup lookup() {
        return new BrokerLookup(CNPJ, "XP INVESTIMENTOS S/A", "XP", "ATIVA", "CTVM",
                "22250911", "Praia Botafogo", "", "Botafogo", "Rio de Janeiro", "RJ");
    }

    private static BrokerAssociationView association() {
        BrokerLookup value = lookup();
        return new BrokerAssociationView(ASSOCIATION_ID, value.cnpj(), value.corporateName(),
                value.tradeName(), value.registrationStatus(), value.cvmCategory(), value.postalCode(),
                value.street(), value.complement(), value.district(), value.city(), value.state());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AccountPrincipal(ACCOUNT_ID), null, List.of()));
    }
}
