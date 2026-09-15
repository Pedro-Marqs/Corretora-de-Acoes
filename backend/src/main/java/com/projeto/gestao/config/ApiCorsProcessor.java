package com.projeto.gestao.config;

import java.io.IOException;

import com.projeto.gestao.api.exception.ApiErrorCode;
import com.projeto.gestao.api.exception.SecurityErrorResponseWriter;

import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.cors.DefaultCorsProcessor;

final class ApiCorsProcessor extends DefaultCorsProcessor {
    private final SecurityErrorResponseWriter errorWriter;

    ApiCorsProcessor(SecurityErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;
        errorWriter.write(servletResponse.getServletResponse(), ApiErrorCode.AUTHORIZATION_ERROR);
    }
}
