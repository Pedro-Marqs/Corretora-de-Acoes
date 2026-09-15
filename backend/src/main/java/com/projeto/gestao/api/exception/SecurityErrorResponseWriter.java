package com.projeto.gestao.api.exception;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX");

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(HttpServletResponse response, ApiErrorCode code) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                code.name(), code.defaultMessage(), List.of(),
                OffsetDateTime.now(clock).format(TIMESTAMP_FORMAT), UUID.randomUUID().toString());
        response.setStatus(code.status().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
