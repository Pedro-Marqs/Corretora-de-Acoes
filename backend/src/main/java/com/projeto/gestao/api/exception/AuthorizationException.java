package com.projeto.gestao.api.exception;

public final class AuthorizationException extends ApiException {
    private static final long serialVersionUID = 1L;

    public AuthorizationException() {
        super(ApiErrorCode.AUTHORIZATION_ERROR);
    }
}
