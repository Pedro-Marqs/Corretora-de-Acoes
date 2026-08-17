package com.projeto.gestao.api.exception;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Os dados informados são inválidos."),
    AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "Não foi possível autenticar a solicitação."),
    AUTHORIZATION_ERROR(HttpStatus.FORBIDDEN, "Você não tem permissão para realizar esta operação."),
    CONFLICT_ERROR(HttpStatus.CONFLICT, "A solicitação está em conflito com os dados existentes."),
    BUSINESS_RULE_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "A operação não atende às regras de negócio."),
    EXTERNAL_DEPENDENCY_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Um serviço necessário está indisponível."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a solicitação.");

    private final HttpStatus status;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
