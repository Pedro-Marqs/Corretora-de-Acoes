package com.projeto.gestao.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.gestao.domain.model.Account;
import com.projeto.gestao.domain.model.AdminUser;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.AdminAuditEventRepository;
import com.projeto.gestao.repository.AdminRefreshTokenRepository;
import com.projeto.gestao.repository.AdminUserRepository;
import com.projeto.gestao.security.AdminJwtTokenService;
import com.projeto.gestao.security.AdminPrincipal;
import com.projeto.gestao.security.JwtTokenService;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AdminAuthenticationTests.EndpointConfiguration.class)
class AdminAuthenticationTests {
    private static final String ADMIN_ISSUER = "corretora-admin-test";
    private static final String ADMIN_AUDIENCE = "corretora-admin-api-test";
    private static final String INVESTOR_ISSUER = "corretora-investor-test";
    private static final String INVESTOR_AUDIENCE = "corretora-investors-test";
    private static final String ADMIN_PASSWORD = "Senha Admin 1!";
    private static final String ADMIN_SECRET = testSecret();
    private static final String INVESTOR_SECRET = testSecret();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AdminRefreshTokenRepository adminRefreshTokenRepository;
    @Autowired private AdminAuditEventRepository adminAuditEventRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AdminJwtTokenService adminJwtTokenService;
    @Autowired private JwtTokenService investorJwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired @Qualifier("adminJwtDecoder") private JwtDecoder adminJwtDecoder;
    @Autowired @Qualifier("adminJwtEncoder") private JwtEncoder adminJwtEncoder;

    private UUID adminId;
    private UUID accountId;
    private String adminEmail;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.enabled", () -> "true");
        registry.add("app.security.jwt.session-compatibility", () -> "false");
        registry.add("app.security.jwt.issuer", () -> INVESTOR_ISSUER);
        registry.add("app.security.jwt.audience", () -> INVESTOR_AUDIENCE);
        registry.add("app.security.jwt.secret", () -> INVESTOR_SECRET);
        registry.add("app.security.admin-jwt.enabled", () -> "true");
        registry.add("app.security.admin-jwt.issuer", () -> ADMIN_ISSUER);
        registry.add("app.security.admin-jwt.audience", () -> ADMIN_AUDIENCE);
        registry.add("app.security.admin-jwt.secret", () -> ADMIN_SECRET);
        registry.add("app.security.admin-jwt.access-token-ttl", () -> "PT10M");
        registry.add("app.security.admin-jwt.refresh-token-ttl", () -> "PT8H");
    }

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminEmail = "admin-" + adminId + "@example.com";
        accountId = UUID.randomUUID();
        adminUserRepository.saveAndFlush(AdminUser.create(
                adminId, adminEmail, passwordEncoder.encode(ADMIN_PASSWORD), OffsetDateTime.now()));
        accountRepository.saveAndFlush(Account.create(
                accountId, "Investidor Admin Isolation", "52998224725", accountId + "@example.com",
                passwordEncoder.encode("Senha Investidor 1!"), new BigDecimal("10000.00"),
                OffsetDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        adminRefreshTokenRepository.deleteAll();
        adminAuditEventRepository.deleteAll();
        if (accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId);
        }
        if (adminUserRepository.existsById(adminId)) {
            adminUserRepository.deleteById(adminId);
        }
    }

    @Test
    void validAdminLoginEmitsExactAdministrativeClaimsAndCookie() throws Exception {
        LoginCredentials login = login();
        Jwt jwt = adminJwtDecoder.decode(login.accessToken());

        assertThat(jwt.getClaims()).containsOnlyKeys(
                "iss", "aud", "sub", "iat", "exp", "jti", "principal_type", "role");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ADMIN_ISSUER);
        assertThat(jwt.getAudience()).containsExactly(ADMIN_AUDIENCE);
        assertThat(jwt.getSubject()).isEqualTo(adminId.toString());
        assertThat(jwt.getClaimAsString("principal_type")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(login.refreshCookie().getValue()).doesNotContain("=");
        assertThat(login.response().getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "Path=/api/admin/auth", "SameSite=Lax");
        assertThat(login.response().getResponse().getCookie("SESSION")).isNull();
    }

    @Test
    void invalidAndInactiveAdminCredentialsReturnUnauthorized() throws Exception {
        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/admin/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(adminEmail, "wrong-password")))
                .andExpect(status().isUnauthorized());

        AdminUser admin = adminUserRepository.findById(adminId).orElseThrow();
        admin.inactivate(OffsetDateTime.now());
        adminUserRepository.saveAndFlush(admin);
        CsrfCredentials inactiveCsrf = csrf();
        mockMvc.perform(post("/api/admin/auth/login").cookie(inactiveCsrf.cookie())
                        .header("X-XSRF-TOKEN", inactiveCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(adminEmail, ADMIN_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEmailIsNormalizedAndMalformedLoginIsRejected() throws Exception {
        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/admin/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + adminEmail.toUpperCase() + "\","
                                + "\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        CsrfCredentials malformedCsrf = csrf();
        mockMvc.perform(post("/api/admin/auth/login").cookie(malformedCsrf.cookie())
                        .header("X-XSRF-TOKEN", malformedCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON).content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminLoginAndCookieMutationsRequireCsrf() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(adminEmail, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden());

        LoginCredentials login = login();
        mockMvc.perform(post("/api/admin/auth/refresh").cookie(login.refreshCookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/auth/logout").cookie(login.refreshCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshRotatesAndLogoutRevokesAdministrativeFamily() throws Exception {
        LoginCredentials login = login();
        MvcResult rotated = refresh(login.refreshCookie())
                .andExpect(status().isNoContent()).andReturn();
        Cookie next = rotated.getResponse().getCookie(AdminAuthenticationController.REFRESH_COOKIE);

        assertThat(next).isNotNull();
        assertThat(next.getValue()).isNotEqualTo(login.refreshCookie().getValue());
        assertThat(rotated.getResponse().getHeader(HttpHeaders.AUTHORIZATION))
                .startsWith("Bearer ");
        refresh(login.refreshCookie()).andExpect(status().isUnauthorized());
        refresh(next).andExpect(status().isUnauthorized());

        LoginCredentials secondLogin = login();
        CsrfCredentials csrf = csrf();
        mockMvc.perform(post("/api/admin/auth/logout")
                        .cookie(secondLogin.refreshCookie(), csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("ADMIN_REFRESH_TOKEN=;")));
        refresh(secondLogin.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredOrInactiveAdministrativeRefreshIsRejectedAndRevoked() throws Exception {
        LoginCredentials expiredLogin = login();
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update("UPDATE admin_refresh_token SET created_at = ?, expires_at = ? "
                        + "WHERE admin_user_id = ?",
                now.minusMinutes(2), now.minusMinutes(1), adminId);
        refresh(expiredLogin.refreshCookie()).andExpect(status().isUnauthorized());

        LoginCredentials inactiveLogin = login();
        AdminUser admin = adminUserRepository.findById(adminId).orElseThrow();
        admin.inactivate(OffsetDateTime.now());
        adminUserRepository.saveAndFlush(admin);
        refresh(inactiveLogin.refreshCookie()).andExpect(status().isUnauthorized());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_refresh_token WHERE admin_user_id = ? "
                        + "AND revoked_at IS NOT NULL", Integer.class, adminId))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void invalidAdministrativeClaimsAreRejected() throws Exception {
        String wrongRole = token("ADMIN", "USER", ADMIN_AUDIENCE, ADMIN_ISSUER,
                Instant.now().minusSeconds(30), Instant.now().plusSeconds(60), Map.of());
        String extraClaim = token("ADMIN", "ADMIN", ADMIN_AUDIENCE, ADMIN_ISSUER,
                Instant.now().minusSeconds(30), Instant.now().plusSeconds(60),
                Map.of("unexpected", "value"));
        String wrongAudience = token("ADMIN", "ADMIN", INVESTOR_AUDIENCE, ADMIN_ISSUER,
                Instant.now().minusSeconds(30), Instant.now().plusSeconds(60), Map.of());
        String expired = token("ADMIN", "ADMIN", ADMIN_AUDIENCE, ADMIN_ISSUER,
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(60), Map.of());

        assertUnauthorizedAdmin(wrongRole);
        assertUnauthorizedAdmin(extraClaim);
        assertUnauthorizedAdmin(wrongAudience);
        assertUnauthorizedAdmin(expired);
    }

    @Test
    void investorAndAdministrativeTokensAreRejectedByTheOtherChain() throws Exception {
        String adminToken = adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();
        String investorToken = investorJwtTokenService.issue(
                new com.projeto.gestao.security.AccountPrincipal(accountId)).tokenValue();

        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + investorToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/t14/investor").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.adminId").value(adminId.toString()));
        mockMvc.perform(get("/api/t14/investor").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + investorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void activeAdminTokenStopsWorkingAfterInactivation() throws Exception {
        String adminToken = adminJwtTokenService.issue(new AdminPrincipal(adminId)).tokenValue();
        AdminUser admin = adminUserRepository.findById(adminId).orElseThrow();
        admin.inactivate(OffsetDateTime.now());
        adminUserRepository.saveAndFlush(admin);

        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminSessionCookieDoesNotAuthenticateAdministrativeArea() throws Exception {
        mockMvc.perform(get("/api/admin/test").sessionAttr("SPRING_SECURITY_CONTEXT", "legacy"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void investorAuthorityCannotAccessAdministrativeArea() throws Exception {
        mockMvc.perform(get("/api/admin/test")).andExpect(status().isForbidden());
    }

    private LoginCredentials login() throws Exception {
        CsrfCredentials csrf = csrf();
        MvcResult response = mockMvc.perform(post("/api/admin/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(adminEmail, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION,
                        org.hamcrest.Matchers.startsWith("Bearer ")))
                .andReturn();
        return new LoginCredentials(response,
                response.getResponse().getHeader(HttpHeaders.AUTHORIZATION).substring("Bearer ".length()),
                response.getResponse().getCookie(AdminAuthenticationController.REFRESH_COOKIE));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie cookie) throws Exception {
        CsrfCredentials csrf = csrf();
        return mockMvc.perform(post("/api/admin/auth/refresh")
                        .cookie(cookie, csrf.cookie()).header("X-XSRF-TOKEN", csrf.token()));
    }

    private void assertUnauthorizedAdmin(String token) throws Exception {
        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private CsrfCredentials csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfCredentials(result.getResponse().getCookie("XSRF-TOKEN"),
                body.path("token").asText());
    }

    private String token(String principalType, String role, String audience, String issuer,
            Instant issuedAt, Instant expiresAt, Map<String, Object> extraClaims) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer).audience(List.of(audience)).subject(adminId.toString())
                .issuedAt(issuedAt).expiresAt(expiresAt).id(UUID.randomUUID().toString())
                .claim("principal_type", principalType).claim("role", role);
        extraClaims.forEach(claims::claim);
        return adminJwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims.build())).getTokenValue();
    }

    private String json(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    private static String testSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private record LoginCredentials(MvcResult response, String accessToken, Cookie refreshCookie) { }
    private record CsrfCredentials(Cookie cookie, String token) { }

    @TestConfiguration
    static class EndpointConfiguration {
        @Bean
        PrivateEndpoints privateEndpoints() {
            return new PrivateEndpoints();
        }
    }

    @RestController
    static class PrivateEndpoints {
        @GetMapping("/api/admin/test")
        Map<String, String> admin(@AuthenticationPrincipal AdminPrincipal principal) {
            return Map.of("adminId", principal.adminId().toString());
        }

        @GetMapping("/api/t14/investor")
        Map<String, String> investor(@AuthenticationPrincipal
                com.projeto.gestao.security.AccountPrincipal principal) {
            return Map.of("accountId", principal.accountId().toString());
        }
    }
}
