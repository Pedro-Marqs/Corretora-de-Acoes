package com.projeto.gestao.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;
import com.projeto.gestao.domain.model.FinancialAmount;

class GlobalExceptionHandlerTests {
    private static final String TIMESTAMP = "2026-08-17T10:00:00-03:00";
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-17T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorTestController())
                .setControllerAdvice(new GlobalExceptionHandler(clock))
                .build();
    }

    @Test
    void returnsEveryFunctionalCategoryUsingTheSameContract() throws Exception {
        assertCategory("authentication", 401, "AUTHENTICATION_ERROR",
                "Não foi possível autenticar a solicitação.");
        assertCategory("authorization", 403, "AUTHORIZATION_ERROR",
                "Você não tem permissão para realizar esta operação.");
        assertCategory("conflict", 409, "CONFLICT_ERROR",
                "A corretora já está cadastrada na conta.");
        assertCategory("business", 422, "BUSINESS_RULE_ERROR",
                "Saldo insuficiente. Valor solicitado: R$ 123.46; saldo disponível: R$ 100.00.");
        assertCategory("external", 503, "EXTERNAL_DEPENDENCY_ERROR",
                "Um serviço necessário está indisponível.");
    }

    @Test
    void typedBusinessAndConflictMessagesContainOnlyControlledInformation() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String response = mockMvc.perform(get("/test-errors/business"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            "Saldo insuficiente. Valor solicitado: R$ 123.46; saldo disponível: R$ 100.00."))
                    .andReturn().getResponse().getContentAsString();

            assertThat(response).doesNotContain(
                    "SELECT", "java.lang.String", "123.456.789-00", "investidor@example.com",
                    "secret-password", "cookie-value", "api-key-value");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .matches("API request rejected with errorId=[0-9a-f-]{36} "
                            + "code=BUSINESS_RULE_ERROR exception=BusinessRuleException");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void convertsInvalidBodyFieldsToValidationError() throws Exception {
        mockMvc.perform(post("/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Os dados informados são inválidos."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Nome obrigatório."))
                .andExpect(jsonPath("$.timestamp").value(TIMESTAMP));
    }

    @Test
    void unreadableJsonIsAValidationErrorWithoutEchoingBodyOrCause() throws Exception {
        String malformed = "{\"name\":\"secret-password\"";
        String malformedResponse = mockMvc.perform(post("/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON).content(malformed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andReturn().getResponse().getContentAsString();

        String incompatible = "{\"name\":{\"password\":\"secret-password\"}}";
        String incompatibleResponse = mockMvc.perform(post("/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON).content(incompatible))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(malformedResponse).doesNotContain("secret-password", "JsonParseException", "Unexpected end");
        assertThat(incompatibleResponse).doesNotContain("secret-password", "MismatchedInputException", "password");
    }

    @Test
    void convertsMissingAndMismatchedParametersToValidationErrors() throws Exception {
        mockMvc.perform(get("/test-errors/parameter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Parâmetro obrigatório."));

        mockMvc.perform(get("/test-errors/parameter").param("amount", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Formato inválido."));
    }

    @Test
    void supportsMethodAndConstraintViolationValidationExceptions() throws Exception {
        mockMvc.perform(get("/test-errors/method-validation").param("amount", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Deve ser positivo."));

        mockMvc.perform(get("/test-errors/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void illegalArgumentIsNotTreatedAsABusinessRule() throws Exception {
        mockMvc.perform(get("/test-errors/illegal-argument"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void unexpectedErrorsReturnOnlyGenericSafeInformationAndSafeLogs() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String response = mockMvc.perform(get("/test-errors/internal"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("Não foi possível concluir a solicitação."))
                    .andExpect(jsonPath("$.fieldErrors").isEmpty())
                    .andExpect(jsonPath("$.timestamp").value(TIMESTAMP))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString();

            assertThat(response)
                    .doesNotContain("RuntimeException", "SELECT", "password", "secret-value",
                            "cookie-value", "stackTrace", "java.lang");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
            String errorId = JsonPath.read(response, "$.errorId");
            assertThatCodeIsUuid(errorId);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .isEqualTo("Unexpected API error with errorId=" + errorId
                            + " code=INTERNAL_ERROR exception=RuntimeException")
                    .doesNotContain("email@example.com", "123.456.789-00", "secret-value");
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void createsAUniqueErrorIdForEveryOccurrence() throws Exception {
        String firstResponse = mockMvc.perform(get("/test-errors/internal"))
                .andReturn().getResponse().getContentAsString();
        String secondResponse = mockMvc.perform(get("/test-errors/internal"))
                .andReturn().getResponse().getContentAsString();

        String firstId = JsonPath.read(firstResponse, "$.errorId");
        String secondId = JsonPath.read(secondResponse, "$.errorId");
        assertThatCodeIsUuid(firstId);
        assertThatCodeIsUuid(secondId);
        assertThat(firstId).isNotEqualTo(secondId);
    }

    private void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value).toString()).isEqualTo(value);
    }

    private void assertCategory(String path, int status, String code, String message) throws Exception {
        mockMvc.perform(get("/test-errors/" + path))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.timestamp").value(TIMESTAMP))
                .andExpect(jsonPath("$.errorId").isString());
    }

    record TestRequest(@NotBlank(message = "Nome obrigatório.") String name) {
    }

    @RestController
    static class ErrorTestController {
        @PostMapping("/test-errors/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test-errors/parameter")
        void parameter(@RequestParam int amount) {
        }

        @GetMapping("/test-errors/method-validation")
        void methodValidation(@RequestParam @Min(value = 1, message = "Deve ser positivo.") int amount) {
        }

        @GetMapping("/test-errors/constraint-violation")
        void constraintViolation() {
            throw new ConstraintViolationException("unsafe raw message", java.util.Set.of());
        }

        @GetMapping("/test-errors/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("must not become a business rule");
        }

        @GetMapping("/test-errors/authentication")
        void authentication() {
            throw new AuthenticationException();
        }

        @GetMapping("/test-errors/authorization")
        void authorization() {
            throw new AuthorizationException();
        }

        @GetMapping("/test-errors/conflict")
        void conflict() {
            throw ConflictException.brokerAlreadyRegistered();
        }

        @GetMapping("/test-errors/business")
        void business() {
            throw BusinessRuleException.insufficientBalance(
                    FinancialAmount.of("123.456"), FinancialAmount.of("100"));
        }

        @GetMapping("/test-errors/external")
        void external() {
            throw new ExternalDependencyException();
        }

        @GetMapping("/test-errors/internal")
        void internal() {
            throw new RuntimeException(
                    "SELECT secret-value password=secret-value cookie=cookie-value "
                            + "email@example.com 123.456.789-00");
        }
    }
}
