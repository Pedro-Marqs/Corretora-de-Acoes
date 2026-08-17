package com.projeto.gestao.api.exception;

public final class AuthenticationException extends ApiException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException() {
        super(ApiErrorCode.AUTHENTICATION_ERROR);
    }
}
