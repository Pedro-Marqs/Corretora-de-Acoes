package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(SecurityConfigTests.TestEndpointsConfiguration.class)
class SecurityConfigTests {
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DefaultCookieSerializer cookieSerializer;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void doesNotCreateDefaultUserWhileKeepingProjectSecurityInfrastructure() {
        assertThat(applicationContext.getBeansOfType(UserDetailsService.class)).isEmpty();
        assertThat(securityFilterChain).isNotNull();
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    void filterChainUsesCookieCsrfRepository() {
        CsrfFilter filter = securityFilterChain.getFilters().stream()
                .filter(CsrfFilter.class::isInstance)
                .map(CsrfFilter.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(ReflectionTestUtils.getField(filter, "tokenRepository"))
                .isInstanceOf(CookieCsrfTokenRepository.class);
    }

    @Test
    void privateRouteRequiresAuthenticationAndUsesStandardErrorContract() throws Exception {
        mockMvc.perform(get("/api/test/private"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errorId").isNotEmpty());

        mockMvc.perform(get("/api/test/private").with(user("investidor")))
                .andExpect(status().isOk());
    }

    @Test
    void mutationRequiresCsrfEvenWhenRouteIsPublic() throws Exception {
        mockMvc.perform(post("/api/accounts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));

        CsrfCredentials csrf = csrfCredentials();
        mockMvc.perform(post("/api/accounts")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Investidor Teste",
                                  "cpf": "52998224725",
                                  "email": "security-config@example.com",
                                  "password": "SenhaSegura1!"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void publicMatchersAreExactByMethodAndPath() throws Exception {
        CsrfCredentials csrf = csrfCredentials();
        mockMvc.perform(post("/api/auth/login").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts/reactivation").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/accounts/reactivation/check").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts/extra").cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())).andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsOnlyConfiguredOriginWithCredentials() throws Exception {
        mockMvc.perform(get("/api/csrf").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(get("/api/csrf").header("Origin", "https://example.invalid"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errorId").isNotEmpty());
    }

    @Test
    void corsPreflightAllowsConfiguredOriginAndRejectsOthersWithStandardError() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/accounts")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods",
                        org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsStringIgnoringCase("X-XSRF-TOKEN")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/accounts")
                        .header("Origin", "https://example.invalid")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_ERROR"));
    }

    @Test
    void csrfEndpointMaterializesCookieWithoutCreatingSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("XSRF-TOKEN=")))
                .andReturn();

        assertThat(result.getResponse().getCookie("SESSION")).isNull();
        Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        assertThat(xsrfCookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("XSRF-TOKEN=", "Path=/")
                .doesNotContain("HttpOnly", "Secure");
    }

    @Test
    void sessionCookieIsHttpOnlyLaxAndNotSecureInTestProfile() {
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse response =
                new org.springframework.mock.web.MockHttpServletResponse();
        cookieSerializer.writeCookieValue(new CookieValue(request, response, "session-id"));

        assertThat(response.getHeader("Set-Cookie"))
                .contains("SESSION=", "Path=/", "HttpOnly", "SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void bcryptHashesAndVerifiesPassword() {
        String hash = passwordEncoder.encode("SenhaSegura123!");
        assertThat(hash).startsWith("$2");
        assertThat(hash).doesNotContain("SenhaSegura123!");
        assertThat(passwordEncoder.matches("SenhaSegura123!", hash)).isTrue();
    }

    @Test
    void authenticatedMockUserCanAccessPrivateRoute() throws Exception {
        mockMvc.perform(get("/api/test/session").with(user("investidor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("investidor"));
    }

    private CsrfCredentials csrfCredentials() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new CsrfCredentials(cookie, body.path("token").asText());
    }

    private record CsrfCredentials(Cookie cookie, String token) {
    }

    @TestConfiguration
    static class TestEndpointsConfiguration {
        @Bean
        TestEndpoints testEndpoints() {
            return new TestEndpoints();
        }
    }

    @RestController
    @RequestMapping("/api")
    static class TestEndpoints {
        @GetMapping("/test/private")
        Map<String, String> privateRoute(@AuthenticationPrincipal UserDetails user) {
            return Map.of("user", user.getUsername());
        }

        @GetMapping("/test/session")
        Map<String, String> sessionRoute(
                @AuthenticationPrincipal UserDetails user, HttpServletRequest request) {
            request.getSession(true);
            return Map.of("user", user.getUsername());
        }
    }
}
