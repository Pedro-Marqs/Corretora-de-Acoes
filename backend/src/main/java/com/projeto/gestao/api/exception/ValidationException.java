package com.projeto.gestao.api.exception;

public final class ValidationException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ValidationException() {
        super(ApiErrorCode.VALIDATION_ERROR);
    }
}
