package com.projeto.gestao.api.exception;

public final class ExternalDependencyException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ExternalDependencyException() {
        super(ApiErrorCode.EXTERNAL_DEPENDENCY_ERROR);
    }
}
