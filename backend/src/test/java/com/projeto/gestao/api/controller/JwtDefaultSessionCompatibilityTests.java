package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.config.JwtSecurityProperties;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JwtDefaultSessionCompatibilityTests.EndpointConfiguration.class)
@Transactional
class JwtDefaultSessionCompatibilityTests {
    private static final String ISSUER = "https://corretora.test/default-session-mode";
    private static final String AUDIENCE = "corretora-investors";
    private static final String EMAIL = "jwt-default-session@example.com";
    private static final String PASSWORD = "Senha Default 1!";
    private static final String TEST_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtSecurityProperties jwtProperties;
    @Autowired private SecurityContextRepository securityContextRepository;
    @SuppressWarnings("rawtypes")
    @Autowired private SessionRepository sessionRepository;
    @Autowired private CookieSerializer cookieSerializer;

    private UUID accountId;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.issuer", () -> ISSUER);
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
        registry.add("app.security.jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void createActiveAccount() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor Default", "52998224725",
                EMAIL, passwordEncoder.encode(PASSWORD), new BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtModeDisablesSessionCompatibilityByDefault() throws Exception {
        assertThat(jwtProperties.sessionCompatibility()).isFalse();
        assertThat(securityContextRepository).isInstanceOf(NullSecurityContextRepository.class);

        CsrfCredentials csrf = csrf();
        MvcResult login = mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(login.getResponse().getCookie("SESSION")).isNull();
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/t13/default-private")).andExpect(status().isUnauthorized());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void preexistingJdbcSessionIsRejectedWhenJwtOnlyModeIsActive() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccountPrincipal(accountId), null, List.of()));
        Session session = sessionRepository.createSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        sessionRepository.save(session);

        MockHttpServletResponse cookieResponse = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(
                new MockHttpServletRequest(), cookieResponse, session.getId()));

        mockMvc.perform(get("/api/t13/default-private")
                        .cookie(cookieResponse.getCookies()[0]))
                .andExpect(status().isUnauthorized());
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
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
        @GetMapping("/api/t13/default-private")
        Map<String, String> privateRoute() {
            return Map.of("status", "ok");
        }
    }
}
