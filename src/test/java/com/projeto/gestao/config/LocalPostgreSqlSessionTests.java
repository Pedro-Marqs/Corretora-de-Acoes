package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@Import(LocalPostgreSqlSessionTests.EndpointConfiguration.class)
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_POSTGRES_TESTS", matches = "(?i)true")
class LocalPostgreSqlSessionTests {
    private static final String DATABASE_NAME = "gestao_acoes_test";

    @DynamicPropertySource
    static void localPostgres(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault(
                "LOCAL_POSTGRES_TEST_URL", "jdbc:postgresql://localhost:5432/" + DATABASE_NAME);
        if (!url.matches("jdbc:postgresql://[^/]+/" + DATABASE_NAME + "(?:\\?.*)?")) {
            throw new IllegalStateException("O teste aceita exclusivamente o banco " + DATABASE_NAME);
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> requiredEnvironment("LOCAL_POSTGRES_TEST_USER"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("LOCAL_POSTGRES_TEST_PASSWORD"));
        registry.add("spring.flyway.locations", () ->
                "classpath:db/migration/common,classpath:db/migration/postgresql");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável obrigatória ausente: " + name);
        }
        return value;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndRecoversSecuritySessionInExclusiveLocalTestDatabase() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/integration/session").with(user("investidor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("investidor"))
                .andReturn();
        Cookie session = first.getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        Integer persisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID IS NOT NULL", Integer.class);
        assertThat(persisted).isPositive();

        mockMvc.perform(get("/api/integration/session").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("investidor"));
    }

    @TestConfiguration
    static class EndpointConfiguration {
        @Bean
        SessionEndpoint sessionEndpoint() {
            return new SessionEndpoint();
        }
    }

    @RestController
    static class SessionEndpoint {
        @GetMapping("/api/integration/session")
        Map<String, String> session(
                @AuthenticationPrincipal UserDetails user, HttpServletRequest request) {
            request.getSession(true);
            return Map.of("user", user.getUsername());
        }
    }
}
