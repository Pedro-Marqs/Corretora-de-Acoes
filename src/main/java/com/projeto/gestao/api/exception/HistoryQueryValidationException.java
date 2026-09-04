package com.projeto.gestao.api.exception;

public final class HistoryQueryValidationException extends ApiException {
    private static final long serialVersionUID = 1L;

    public HistoryQueryValidationException() {
        super(ApiErrorCode.VALIDATION_ERROR);
    }
}
