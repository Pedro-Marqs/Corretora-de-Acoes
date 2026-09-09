package com.projeto.gestao.api.exception;

public abstract class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ApiErrorCode errorCode;

    protected ApiException(ApiErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public String publicMessage() {
        return errorCode.defaultMessage();
    }
}
