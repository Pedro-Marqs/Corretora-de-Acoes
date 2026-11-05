package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.security.AccountPrincipal;
import com.projeto.gestao.security.JwtTokenService;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.crypto.SecretKey;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JwtAuthenticationTests.EndpointConfiguration.class)
@Transactional
class JwtAuthenticationTests {
    private static final String ISSUER = "https://corretora.test/issuer";
    private static final String AUDIENCE = "corretora-investors";
    private static final String EMAIL = "jwt-login@example.com";
    private static final String PASSWORD = "Senha JWT 1!";
    private static final String TEST_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JwtDecoder jwtDecoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private SecretKey jwtSecretKey;

    private UUID accountId;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.issuer", () -> ISSUER);
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
        registry.add("app.security.jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.jwt.secret", () -> TEST_SECRET);
    }

    @BeforeEach
    void createActiveAccount() {
        accountId = UUID.randomUUID();
        accountRepository.saveAndFlush(Account.create(accountId, "Investidor JWT", "52998224725",
                EMAIL, passwordEncoder.encode(PASSWORD), new java.math.BigDecimal("10000.00"),
                OffsetDateTime.parse("2026-01-01T10:00:00-03:00")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerAuthenticatesOnlyTheTokenAccount() throws Exception {
        String token = jwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void missingInvalidAndTamperedBearerReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/t13/private")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());

        String token = jwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();
        String[] tokenParts = token.split("\\.");
        String signature = tokenParts[2];
        char firstSignatureCharacter = signature.charAt(0);
        char replacement = firstSignatureCharacter == 'a' ? 'b' : 'a';
        String tampered = tokenParts[0] + "." + tokenParts[1] + "."
                + replacement + signature.substring(1);
        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredOrWrongInvestorClaimsReturnUnauthorized() throws Exception {
        String expired = token(Instant.now().minus(Duration.ofMinutes(10)),
                Instant.now().minus(Duration.ofMinutes(1)), AUDIENCE, "INVESTOR", "USER");
        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        String wrongAudience = token(Instant.now().minus(Duration.ofMinutes(1)),
                Instant.now().plus(Duration.ofMinutes(5)), "administrators", "INVESTOR", "USER");
        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + wrongAudience))
                .andExpect(status().isUnauthorized());

        String wrongPrincipalType = token(Instant.now().minus(Duration.ofMinutes(1)),
                Instant.now().plus(Duration.ofMinutes(5)), AUDIENCE, "ADMIN", "ADMIN");
        mockMvc.perform(get("/api/t13/private")
                        .header("Authorization", "Bearer " + wrongPrincipalType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerIsRejected() throws Exception {
        String token = token("https://wrong.example/issuer", AUDIENCE, accountId.toString(),
                Instant.now().minus(Duration.ofMinutes(1)), Instant.now().plus(Duration.ofMinutes(5)),
                UUID.randomUUID().toString(), "INVESTOR", "USER", null, null);

        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiredSubjectTemporalAndJtiClaimsAreEnforced() throws Exception {
        assertUnauthorized(token(ISSUER, AUDIENCE, accountId.toString(),
                Instant.now().minus(Duration.ofMinutes(1)), Instant.now().plus(Duration.ofMinutes(5)),
                null, "INVESTOR", "USER", null, null));
        assertUnauthorized(token(ISSUER, AUDIENCE, accountId.toString(),
                Instant.now().minus(Duration.ofMinutes(1)), Instant.now().plus(Duration.ofMinutes(5)),
                "not-a-uuid", "INVESTOR", "USER", null, null));
        assertUnauthorized(token(ISSUER, AUDIENCE, accountId.toString(), null,
                Instant.now().plus(Duration.ofMinutes(5)), UUID.randomUUID().toString(),
                "INVESTOR", "USER", "not-an-instant", null));
        assertUnauthorized(token(ISSUER, AUDIENCE, accountId.toString(), null,
                Instant.now().plus(Duration.ofMinutes(5)), UUID.randomUUID().toString(),
                "INVESTOR", "USER", null, null));
        assertUnauthorized(token(ISSUER, AUDIENCE, accountId.toString(),
                Instant.now().minus(Duration.ofMinutes(1)), null, UUID.randomUUID().toString(),
                "INVESTOR", "USER", null, "not-an-instant"));
        assertUnauthorized(token(ISSUER, AUDIENCE, accountId.toString(),
                Instant.now().plus(Duration.ofHours(2)), Instant.now().plus(Duration.ofHours(3)),
                UUID.randomUUID().toString(), "INVESTOR", "USER", null, null));
        assertUnauthorized(token(ISSUER, AUDIENCE, "not-a-uuid",
                Instant.now().minus(Duration.ofMinutes(1)), Instant.now().plus(Duration.ofMinutes(5)),
                UUID.randomUUID().toString(), "INVESTOR", "USER", null, null));
    }

    @Test
    void bearerPreflightAndMutationFollowCorsAndCsrfPolicy() throws Exception {
        String token = jwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(options("/api/t13/private")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsStringIgnoringCase("Authorization")));

        mockMvc.perform(post("/api/t13/private")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenForInactivatedAccountReturnsUnauthorized() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.inactivate(OffsetDateTime.parse("2026-01-02T10:00:00-03:00"));
        accountRepository.saveAndFlush(account);

        String token = jwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuedTokenContainsOnlyInvestorClaimsAndLoginReturnsBearerHeader() throws Exception {
        String token = jwtTokenService.issue(new AccountPrincipal(accountId)).tokenValue();
        var claims = jwtDecoder.decode(token);
        assertThat(claims.getIssuer().toString()).isEqualTo(ISSUER);
        assertThat(claims.getAudience()).containsExactly(AUDIENCE);
        assertThat(claims.getSubject()).isEqualTo(accountId.toString());
        assertThat(claims.getClaimAsString("principal_type")).isEqualTo("INVESTOR");
        assertThat(claims.getClaimAsString("role")).isEqualTo("USER");
        assertThat(claims.getId()).isNotBlank();
        assertThat(UUID.fromString(claims.getId())).isNotNull();
        assertThat(claims.getExpiresAt()).isAfter(claims.getIssuedAt());
        assertThat(claims.getClaims()).doesNotContainKeys("email", "cpf", "password", "balance");

        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    private String token(Instant issuedAt, Instant expiresAt, String audience,
            String principalType, String role) {
        return token(ISSUER, audience, accountId.toString(), issuedAt, expiresAt,
                UUID.randomUUID().toString(), principalType, role, null, null);
    }

    private String token(String issuer, String audience, String subject,
            Instant issuedAt, Instant expiresAt, String jti,
            String principalType, String role, Object rawIat, Object rawExp) {
        if (rawIat != null || rawExp != null) {
            return rawToken(issuer, audience, subject, issuedAt, expiresAt, jti,
                    principalType, role, rawIat, rawExp);
        }
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(subject)
                .claim("principal_type", principalType)
                .claim("role", role);
        if (rawIat != null) {
            claims.claim("iat", rawIat);
        } else if (issuedAt != null) {
            claims.issuedAt(issuedAt);
        }
        if (rawExp != null) {
            claims.claim("exp", rawExp);
        } else if (expiresAt != null) {
            claims.expiresAt(expiresAt);
        }
        if (jti != null) {
            claims.id(jti);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims.build())).getTokenValue();
    }

    private String rawToken(String issuer, String audience, String subject,
            Instant issuedAt, Instant expiresAt, String jti,
            String principalType, String role, Object rawIat, Object rawExp) {
        try {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", issuer);
            claims.put("aud", List.of(audience));
            claims.put("sub", subject);
            if (rawIat != null) {
                claims.put("iat", rawIat);
            } else if (issuedAt != null) {
                claims.put("iat", issuedAt.getEpochSecond());
            }
            if (rawExp != null) {
                claims.put("exp", rawExp);
            } else if (expiresAt != null) {
                claims.put("exp", expiresAt.getEpochSecond());
            }
            if (jti != null) claims.put("jti", jti);
            claims.put("principal_type", principalType);
            claims.put("role", role);
            com.nimbusds.jose.JWSObject object = new com.nimbusds.jose.JWSObject(
                    new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.HS256)
                            .type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                    new com.nimbusds.jose.Payload(objectMapper.writeValueAsString(claims)));
            object.sign(new com.nimbusds.jose.crypto.MACSigner(jwtSecretKey.getEncoded()));
            return object.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create raw test token", exception);
        }
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/t13/private").header("Authorization", "Bearer " + token))
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
        @GetMapping("/api/t13/private")
        Map<String, String> privateRoute(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/t13/private")
        Map<String, String> privateMutation(@AuthenticationPrincipal AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }
    }
}
